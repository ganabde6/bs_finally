package com.zhixue.ai.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.constant.SystemConstants;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.common.result.ResultCode;
import com.zhixue.ai.module.ai.engine.*;
import com.zhixue.ai.module.ai.entity.*;
import com.zhixue.ai.module.ai.mapper.*;
import com.zhixue.ai.module.exam.entity.*;
import com.zhixue.ai.module.exam.mapper.*;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 业务服务(批改/错题/学情/助学/风控/配置)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final CorrectEngine correctEngine;
    private final AnalysisEngine analysisEngine;
    private final RiskEngine riskEngine;
    private final VariantEngine variantEngine;
    private final AiServiceProvider aiServiceProvider;

    private final AiCorrectRecordMapper correctMapper;
    private final AiErrorBookMapper errorBookMapper;
    private final AiVariantQuestionMapper variantMapper;
    private final AiStudyAnalysisMapper studyAnalysisMapper;
    private final AiClassAnalysisMapper classAnalysisMapper;
    private final AiTutorChatMapper chatMapper;
    private final AiModelConfigMapper aiConfigMapper;
    private final ExamRiskLogMapper riskLogMapper;

    private final ExamAnswerMapper answerMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final ExamQuestionMapper questionMapper;
    private final SysUserMapper userMapper;

    // ===================== AI 批改 =====================

    /**
     * 触发整卷 AI 批改
     * @param answerId 作答记录ID
     * @param answers  questionId -> 学生答案
     */
    @Transactional
    public Map<String, Object> correctAnswer(Long answerId, Map<Long, String> answers) {
        ExamAnswer answer = answerMapper.selectById(answerId);
        if (answer == null) throw new BizException("作答记录不存在");
        // 清理旧批改记录(支持重复批改)
        correctMapper.delete(new LambdaQueryWrapper<AiCorrectRecord>()
                .eq(AiCorrectRecord::getAnswerId, answerId));
        // 逐题批改
        List<ExamPaperQuestion> pqList = paperQuestionMapper.selectByPaperId(answer.getPaperId());
        BigDecimal totalScore = BigDecimal.ZERO;
        int correctCount = 0, wrongCount = 0;
        List<Map<String, Object>> details = new ArrayList<>();
        for (ExamPaperQuestion pq : pqList) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            String studentAnswer = answers.getOrDefault(pq.getQuestionId(), "");
            AiCorrectRecord record = correctEngine.correct(q, studentAnswer, pq.getScore());
            record.setAnswerId(answerId);
            correctMapper.insert(record);
            totalScore = totalScore.add(record.getScore());
            if (record.getIsCorrect() != null && record.getIsCorrect() == 1) correctCount++;
            else wrongCount++;
            // 错题自动归档
            if (record.getIsCorrect() == null || record.getIsCorrect() != 1) {
                archiveErrorBook(answer.getStudentId(), q, answer.getPaperId(), record);
            }
            Map<String, Object> d = new HashMap<>();
            d.put("questionId", q.getId());
            d.put("score", record.getScore());
            d.put("fullScore", record.getFullScore());
            d.put("isCorrect", record.getIsCorrect());
            details.add(d);
        }
        // 更新作答总分与状态
        answer.setTotalScore(totalScore);
        answer.setStatus(SystemConstants.ANSWER_STATUS_CORRECTED);
        answerMapper.updateById(answer);
        // WebSocket 推送批改结果给学生
        try {
            WebSocketServer.sendToUser(answer.getStudentId(),
                    Map.of("type", "correct_finish", "answerId", answerId, "score", totalScore));
        } catch (Exception ignored) {}
        Map<String, Object> result = new HashMap<>();
        result.put("answerId", answerId);
        result.put("totalScore", totalScore);
        result.put("correctCount", correctCount);
        result.put("wrongCount", wrongCount);
        result.put("details", details);
        return result;
    }

    /** 归档错题到错题本 */
    private void archiveErrorBook(Long studentId, ExamQuestion q, Long paperId, AiCorrectRecord record) {
        // 检查是否已存在
        AiErrorBook exist = errorBookMapper.selectOne(new LambdaQueryWrapper<AiErrorBook>()
                .eq(AiErrorBook::getStudentId, studentId)
                .eq(AiErrorBook::getQuestionId, q.getId())
                .last("LIMIT 1"));
        if (exist != null) return; // 已归档
        AiErrorBook eb = new AiErrorBook();
        eb.setStudentId(studentId);
        eb.setQuestionId(q.getId());
        eb.setPaperId(paperId);
        eb.setCorrectId(record.getId());
        eb.setErrorType(decideErrorType(q, record));
        eb.setKnowledgePoint(q.getKnowledgePoint());
        eb.setReviewStatus(0);
        errorBookMapper.insert(eb);
    }

    private Integer decideErrorType(ExamQuestion q, AiCorrectRecord r) {
        Integer type = q.getQuestionType();
        if (type == null) return 1;
        // 客观题:多为知识点缺失或审题错误
        if (type <= SystemConstants.Q_TYPE_FILL) return 3;
        // 计算题:计算失误
        if (type == SystemConstants.Q_TYPE_CALC) return 2;
        // 简答/作文:表达不清
        return 5;
    }

    /** 教师手动微调批改 */
    public void adjustCorrect(Long correctId, BigDecimal newScore, String remark) {
        AiCorrectRecord r = correctMapper.selectById(correctId);
        if (r == null) throw new BizException("批改记录不存在");
        r.setScore(newScore);
        r.setCorrectRemark(remark);
        r.setCorrectType(SystemConstants.CORRECT_TYPE_MANUAL);
        if (newScore.compareTo(BigDecimal.ZERO) == 0) r.setIsCorrect(0);
        else if (newScore.compareTo(r.getFullScore()) >= 0) r.setIsCorrect(1);
        else r.setIsCorrect(2);
        correctMapper.updateById(r);
        // 重新计算总分
        recalcAnswerTotal(r.getAnswerId());
    }

    /** 重新计算作答总分 */
    private void recalcAnswerTotal(Long answerId) {
        List<AiCorrectRecord> records = correctMapper.selectByAnswerId(answerId);
        BigDecimal total = records.stream()
                .map(AiCorrectRecord::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ExamAnswer a = new ExamAnswer();
        a.setId(answerId);
        a.setTotalScore(total);
        answerMapper.updateById(a);
    }

    /** 查询批改详情 */
    public List<Map<String, Object>> getCorrectDetails(Long answerId) {
        List<AiCorrectRecord> records = correctMapper.selectByAnswerId(answerId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiCorrectRecord r : records) {
            ExamQuestion q = questionMapper.selectById(r.getQuestionId());
            Map<String, Object> m = new HashMap<>();
            m.put("record", r);
            m.put("question", q);
            result.add(m);
        }
        return result;
    }

    /** 批量批改试卷所有已提交作答 */
    @Transactional
    public Map<String, Object> batchCorrectPaper(Long paperId) {
        List<ExamAnswer> answers = answerMapper.selectByPaperId(paperId);
        int success = 0, skip = 0;
        for (ExamAnswer a : answers) {
            if (a.getStatus() == null || a.getStatus() < SystemConstants.ANSWER_STATUS_SUBMITTED) {
                skip++;
                continue;
            }
            // 已批改的也重新批改
            Map<Long, String> empty = new HashMap<>(); // 占位,因为批改时已写入 studentAnswer
            // 这里需要重新读取已有答案(已在 ai_correct_record.student_answer)
            // 为简化,跳过无答案的
            List<AiCorrectRecord> existRecords = correctMapper.selectByAnswerId(a.getId());
            if (!existRecords.isEmpty()) {
                Map<Long, String> answersMap = existRecords.stream()
                        .collect(Collectors.toMap(AiCorrectRecord::getQuestionId,
                                AiCorrectRecord::getStudentAnswer, (x, y) -> x));
                correctAnswer(a.getId(), answersMap);
                success++;
            } else {
                skip++;
            }
        }
        return Map.of("total", answers.size(), "success", success, "skip", skip);
    }

    // ===================== 错题本 =====================

    public List<Map<String, Object>> listErrorBooks(Long studentId) {
        List<AiErrorBook> list = errorBookMapper.selectByStudentId(studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiErrorBook eb : list) {
            ExamQuestion q = questionMapper.selectById(eb.getQuestionId());
            Map<String, Object> m = new HashMap<>();
            m.put("errorBook", eb);
            m.put("question", q);
            result.add(m);
        }
        return result;
    }

    /** 标记错题已复盘 */
    public void reviewErrorBook(Long id, Integer status) {
        AiErrorBook eb = new AiErrorBook();
        eb.setId(id);
        eb.setReviewStatus(status);
        errorBookMapper.updateById(eb);
    }

    /** 删除错题 */
    public void deleteErrorBook(Long id, Long studentId) {
        AiErrorBook eb = errorBookMapper.selectById(id);
        if (eb == null) throw new BizException("错题不存在");
        if (!eb.getStudentId().equals(studentId)) throw new BizException("无权操作");
        errorBookMapper.deleteById(id);
    }

    /** AI 推送变式题(优先题库匹配,无则 AI 生成,支持批量) */
    public List<AiVariantQuestion> pushVariant(Long errorBookId, Integer count) {
        AiErrorBook eb = errorBookMapper.selectById(errorBookId);
        if (eb == null) throw new BizException("错题不存在");
        ExamQuestion q = questionMapper.selectById(eb.getQuestionId());
        int n = count == null || count < 1 ? 1 : Math.min(count, 10);
        List<AiVariantQuestion> result = new ArrayList<>();
        Set<Long> usedVariantIds = new HashSet<>();

        for (int i = 0; i < n; i++) {
            // 优先从题库匹配变式题(跳过已用过的)
            AiVariantQuestion vq = variantEngine.pushVariant(q, eb.getStudentId());
            if (vq != null && !usedVariantIds.contains(vq.getId())) {
                usedVariantIds.add(vq.getId());
                variantMapper.insert(vq);
                result.add(vq);
                continue;
            }
            // 题库无匹配或已用完,调用 AI 生成变式题
            log.info("题库无合适变式题,调用 AI 生成第{}道, 知识点:{}, 题型:{}", i + 1, q.getKnowledgePoint(), q.getQuestionType());
            String aiVariant = aiServiceProvider.generateVariant(q.getContent(), q.getKnowledgePoint(), q.getQuestionType(), i + 1);
            AiVariantQuestion aiVq = new AiVariantQuestion();
            aiVq.setSourceQuestionId(q.getId());
            aiVq.setStudentId(eb.getStudentId());
            aiVq.setContent(aiVariant);
            aiVq.setKnowledgePoint(q.getKnowledgePoint());
            aiVq.setIsSolved(0);
            variantMapper.insert(aiVq);
            result.add(aiVq);
        }
        return result;
    }

    /** 变式题作答 + AI 批改 */
    public Map<String, Object> submitVariantAnswer(Long variantId, Long studentId, String answer, java.util.List<String> images) {
        AiVariantQuestion vq = variantMapper.selectById(variantId);
        if (vq == null) throw new BizException("变式题不存在");
        if (!vq.getStudentId().equals(studentId)) throw new BizException("无权操作他人的变式题");
        if (vq.getIsSolved() != null && vq.getIsSolved() == 1) throw new BizException("该变式题已作答");
        if ((answer == null || answer.trim().isEmpty()) && (images == null || images.isEmpty())) {
            throw new BizException("请先填写答案或上传草稿纸照片");
        }

        // 标准答案优先取字段;AI 生成的变式题答案内嵌在 content 的【答案】段中
        String std = vq.getStandardAnswer();
        if (std == null || std.trim().isEmpty()) {
            std = extractAnswerFromContent(vq.getContent());
        }
        
        // 如果有图片，使用多模态接口让 AI 识别图片中的解题过程
        String result;
        if (images != null && !images.isEmpty()) {
            result = aiServiceProvider.correctVariantWithImages(vq.getContent(), std, answer, images);
        } else {
            result = aiServiceProvider.correctVariant(vq.getContent(), std, answer != null ? answer.trim() : "");
        }
        boolean correct = result != null && result.startsWith("正确");

        // 保存学生答案和图片
        String fullAnswer = answer != null ? answer.trim() : "";
        vq.setStudentAnswer(fullAnswer);
        
        // 保存图片数据
        if (images != null && !images.isEmpty()) {
            vq.setStudentImages(com.alibaba.fastjson2.JSON.toJSONString(images));
        } else {
            vq.setStudentImages(null);
        }
        vq.setIsSolved(1);
        vq.setIsCorrect(correct ? 1 : 0);
        variantMapper.updateById(vq);

        Map<String, Object> r = new HashMap<>();
        r.put("correct", correct);
        r.put("feedback", result == null ? "AI 暂时无法判定,请对照答案自查" : result);
        return r;
    }

    /** 从 AI 生成的变式题内容中提取【答案】段 */
    private String extractAnswerFromContent(String content) {
        if (content == null) return null;
        int a = content.indexOf("【答案】");
        if (a < 0) return null;
        int b = content.indexOf("【解析】", a);
        String s = b > a ? content.substring(a + 4, b) : content.substring(a + 4);
        return s.trim();
    }

    /** 学生变式题列表 */
    public List<AiVariantQuestion> listVariants(Long studentId) {        return variantMapper.selectList(new LambdaQueryWrapper<AiVariantQuestion>()
                .eq(AiVariantQuestion::getStudentId, studentId)
                .orderByDesc(AiVariantQuestion::getCreateTime));
    }

    /** 删除变式题 */
    public void deleteVariant(Long variantId, Long studentId) {
        AiVariantQuestion vq = variantMapper.selectById(variantId);
        if (vq == null) throw new BizException("变式题不存在");
        if (!vq.getStudentId().equals(studentId)) throw new BizException("无权删除他人的变式题");
        variantMapper.deleteById(variantId);
    }

    // ===================== 学情分析 =====================

    public AiStudyAnalysis getStudentAnalysis(Long studentId, Long subjectId) {
        return analysisEngine.analyzeStudent(studentId, subjectId);
    }

    public AiClassAnalysis getClassAnalysis(Long classId, Long subjectId) {
        return analysisEngine.analyzeClass(classId, subjectId);
    }

    /** 生成个性化评语 */
    public String generateComment(Long studentId) {
        SysUser u = userMapper.selectById(studentId);
        AiStudyAnalysis sa = studyAnalysisMapper.selectOne(new LambdaQueryWrapper<AiStudyAnalysis>()
                .eq(AiStudyAnalysis::getStudentId, studentId).last("LIMIT 1"));
        String perf = sa == null ? "" : "平均分:" + sa.getAvgScore() +
                ",薄弱点:" + (sa.getWeakPoints() == null ? "无" : sa.getWeakPoints());
        return aiServiceProvider.generateComment(u == null ? null : u.getRealName(), perf);
    }

    // ===================== AI 助学 =====================

    /** AI 答疑(文字/拍照/语音) */
    @Transactional
    public Map<String, Object> tutorChat(Long studentId, String question, Integer chatType, String imageBase64) {
        // 内容风控
        String risk = aiServiceProvider.moderateContent(question);
        if (risk != null) {
            throw new BizException(ResultCode.CONTENT_RISK_BLOCK.getCode(), risk);
        }
        // 查询开关
        if (!isAiFeatureEnabled("enable_tutor")) {
            throw new BizException("AI 助学功能已关闭");
        }
        // 拼接学情上下文
        AiStudyAnalysis sa = studyAnalysisMapper.selectOne(new LambdaQueryWrapper<AiStudyAnalysis>()
                .eq(AiStudyAnalysis::getStudentId, studentId).last("LIMIT 1"));
        String context = sa == null ? "" :
                "薄弱点:" + sa.getWeakPoints() + ",优势:" + sa.getStrongPoints();
        // AI 回答（有图片时使用多模态接口）
        String answer;
        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            java.util.List<String> images = java.util.Collections.singletonList(imageBase64);
            answer = aiServiceProvider.tutorAnswerWithImages(question, context, images);
        } else {
            answer = aiServiceProvider.tutorAnswer(question, context);
        }
        // 保存对话记录
        AiTutorChat userMsg = new AiTutorChat();
        userMsg.setStudentId(studentId);
        userMsg.setRole("user");
        userMsg.setContent(question);
        userMsg.setChatType(chatType == null ? 1 : chatType);
        chatMapper.insert(userMsg);
        AiTutorChat aiMsg = new AiTutorChat();
        aiMsg.setStudentId(studentId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(answer);
        aiMsg.setChatType(1);
        chatMapper.insert(aiMsg);
        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("chatId", aiMsg.getId());
        return result;
    }

    /** AI 作文润色 */
    public String polishEssay(String original, String imageBase64) {
        if (!isAiFeatureEnabled("enable_polish")) {
            throw new BizException("智能润色功能已关闭");
        }
        // 如果有图片，使用多模态接口识别手写内容后再润色
        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            java.util.List<String> images = java.util.Collections.singletonList(imageBase64);
            return aiServiceProvider.polishTextWithImage(original, images);
        }
        return aiServiceProvider.polishText(original);
    }

    /** 历史对话 */
    public List<AiTutorChat> listChatHistory(Long studentId) {
        return chatMapper.selectByStudentId(studentId);
    }

    // ===================== 风控 =====================

    public List<ExamRiskLog> listRiskLogs(Long answerId) {
        return riskLogMapper.selectByAnswerId(answerId);
    }

    public ExamRiskLog reportRisk(Long answerId, Long studentId, Integer riskType, String desc) {
        return riskEngine.recordRisk(answerId, studentId, riskType, desc);
    }

    /** 触发答案雷同查重 */
    public int similarityCheck(Long answerId) {
        return riskEngine.similarityCheck(answerId);
    }

    // ===================== AI 模型配置 =====================

    public List<AiModelConfig> listAiConfigs() {
        return aiConfigMapper.selectList(null);
    }

    public void updateAiConfig(AiModelConfig config) {
        aiConfigMapper.updateById(config);
    }

    public String getAiConfigValue(String key) {
        AiModelConfig c = aiConfigMapper.selectOne(new LambdaQueryWrapper<AiModelConfig>()
                .eq(AiModelConfig::getConfigKey, key));
        return c == null ? null : c.getConfigValue();
    }

    public boolean isAiFeatureEnabled(String key) {
        String v = getAiConfigValue(key);
        return "true".equalsIgnoreCase(v);
    }
}
