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
        r.setReviewStatus(0); // 教师已人工确认,清除存疑标记
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
            // 题目可能已被删除,跳过避免前端空指针
            if (q == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("record", r);
            m.put("question", q);
            result.add(m);
        }
        return result;
    }

    /**
     * 题目维度:统计某班级所有学生对该题的作答情况
     * @param questionId 题目ID
     * @param classId    班级ID
     */
    public Map<String, Object> getQuestionClassAnswers(Long questionId, Long classId) {
        // 班级全部学生
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, classId)
                .eq(SysUser::getRoleId, 4L)
                .orderByAsc(SysUser::getUsername));
        // 该题在所有作答中的批改记录(该题答题记录按 answer -> student 关联)
        List<AiCorrectRecord> records = correctMapper.selectList(new LambdaQueryWrapper<AiCorrectRecord>()
                .eq(AiCorrectRecord::getQuestionId, questionId));
        Map<Long, AiCorrectRecord> byAnswer = records.stream()
                .collect(Collectors.toMap(AiCorrectRecord::getAnswerId, r -> r, (a, b) -> a));
        // 学生 -> 作答记录
        Map<Long, ExamAnswer> answerByStudent = new HashMap<>();
        if (!byAnswer.isEmpty()) {
            List<ExamAnswer> answers = answerMapper.selectBatchIds(byAnswer.keySet());
            answers.forEach(a -> answerByStudent.put(a.getStudentId(), a));
        }
        // 汇总每名学生的作答
        List<Map<String, Object>> rows = new ArrayList<>();
        int correct = 0, wrong = 0, unanswered = 0;
        for (SysUser stu : students) {
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", stu.getId());
            row.put("studentName", stu.getRealName());
            row.put("username", stu.getUsername());
            ExamAnswer ans = answerByStudent.get(stu.getId());
            AiCorrectRecord r = ans != null ? byAnswer.get(ans.getId()) : null;
            if (ans == null || ans.getStatus() == null || ans.getStatus() < 1) {
                row.put("status", "未作答");
                row.put("studentAnswer", "");
                row.put("score", null);
                row.put("isCorrect", null);
                unanswered++;
            } else if (r == null) {
                row.put("status", "已提交待批改");
                row.put("studentAnswer", "");
                row.put("score", null);
                row.put("isCorrect", null);
            } else {
                row.put("status", "已批改");
                row.put("studentAnswer", r.getStudentAnswer());
                row.put("score", r.getScore());
                row.put("isCorrect", r.getIsCorrect());
                row.put("remark", r.getCorrectRemark());
                if (r.getIsCorrect() != null && r.getIsCorrect() == 1) correct++;
                else wrong++;
            }
            rows.add(row);
        }
        Map<String, Object> stat = new HashMap<>();
        stat.put("total", students.size());
        stat.put("correct", correct);
        stat.put("wrong", wrong);
        stat.put("unanswered", unanswered);
        stat.put("correctRate", students.isEmpty() ? "0%" :
                Math.round(correct * 1000.0 / students.size()) / 10.0 + "%");
        Map<String, Object> result = new HashMap<>();
        result.put("stat", stat);
        result.put("rows", rows);
        return result;
    }

    /**
     * 试卷维度:整套试卷每题的全班作答情况(逐人明细 + 客观题选项分布标注)
     * @param paperId 试卷ID(取其所属班级学生)
     */
    public Map<String, Object> getPaperClassAnswers(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BizException("试卷不存在");
        Long classId = paper.getClassId();

        // 班级全部学生
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getClassId, classId)
                .eq(SysUser::getRoleId, 4L)
                .orderByAsc(SysUser::getUsername));
        Map<Long, SysUser> stuMap = students.stream()
                .collect(Collectors.toMap(SysUser::getId, s -> s, (a, b) -> a));

        // 该试卷全部作答(每学生取最新一条)
        List<ExamAnswer> answers = answerMapper.selectByPaperId(paperId);
        Map<Long, ExamAnswer> answerByStudent = new HashMap<>();
        for (ExamAnswer a : answers) {
            answerByStudent.merge(a.getStudentId(), a,
                    (oldV, newV) -> newV.getId() > oldV.getId() ? newV : oldV);
        }

        // 批量取这些作答的全部批改记录,按 answerId+questionId 索引
        Map<Long, Map<Long, AiCorrectRecord>> recordsByAnswer = new HashMap<>();
        if (!answers.isEmpty()) {
            List<Long> answerIds = answers.stream().map(ExamAnswer::getId).collect(Collectors.toList());
            List<AiCorrectRecord> recs = correctMapper.selectList(new LambdaQueryWrapper<AiCorrectRecord>()
                    .in(AiCorrectRecord::getAnswerId, answerIds));
            for (AiCorrectRecord r : recs) {
                recordsByAnswer.computeIfAbsent(r.getAnswerId(), k -> new HashMap<>()).put(r.getQuestionId(), r);
            }
        }

        // 试卷题目(按 sort 顺序)
        List<ExamPaperQuestion> pqs = paperQuestionMapper.selectList(new LambdaQueryWrapper<ExamPaperQuestion>()
                .eq(ExamPaperQuestion::getPaperId, paperId)
                .orderByAsc(ExamPaperQuestion::getSort));

        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion pq : pqs) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            if (q == null) continue;
            int type = q.getQuestionType() == null ? 0 : q.getQuestionType();
            boolean objective = type == SystemConstants.Q_TYPE_SINGLE
                    || type == SystemConstants.Q_TYPE_MULTI
                    || type == SystemConstants.Q_TYPE_JUDGE
                    || type == SystemConstants.Q_TYPE_FILL;

            // 每个学生的作答明细
            List<Map<String, Object>> rows = new ArrayList<>();
            // 客观题选项分布(仅客观题): key=A/B/C/D -> 人数
            Map<String, Integer> optionDist = new LinkedHashMap<>();
            int answered = 0, correctCnt = 0;
            BigDecimal scoreSum = BigDecimal.ZERO;
            int submitted = 0;
            for (SysUser stu : students) {
                Map<String, Object> row = new HashMap<>();
                row.put("studentId", stu.getId());
                row.put("studentName", stu.getRealName());
                row.put("username", stu.getUsername());
                ExamAnswer ans = answerByStudent.get(stu.getId());
                AiCorrectRecord r = (ans != null && ans.getStatus() != null && ans.getStatus() >= 1)
                        ? recordsByAnswer.getOrDefault(ans.getId(), Collections.emptyMap()).get(q.getId()) : null;
                if (ans == null || ans.getStatus() == null || ans.getStatus() < 1) {
                    row.put("status", "未作答");
                    row.put("studentAnswer", "");
                    row.put("score", null);
                    row.put("isCorrect", null);
                } else {
                    submitted++;
                    row.put("status", r != null ? "已批改" : "已提交待批改");
                    row.put("studentAnswer", r != null ? r.getStudentAnswer() : "");
                    row.put("score", r != null ? r.getScore() : null);
                    row.put("isCorrect", r != null ? r.getIsCorrect() : null);
                    if (r != null) {
                        answered++;
                        scoreSum = scoreSum.add(r.getScore() == null ? BigDecimal.ZERO : r.getScore());
                        if (r.getIsCorrect() != null && r.getIsCorrect() == 1) correctCnt++;
                        // 客观题统计选项分布
                        if (objective && r.getStudentAnswer() != null) {
                            String sa = r.getStudentAnswer().trim().toUpperCase();
                            if (type == SystemConstants.Q_TYPE_MULTI) {
                                for (char c : sa.replaceAll("[^A-Z]", "").toCharArray()) {
                                    optionDist.merge(String.valueOf(c), 1, Integer::sum);
                                }
                            } else if (!sa.isEmpty()) {
                                optionDist.merge(sa, 1, Integer::sum);
                            }
                        }
                    }
                }
                rows.add(row);
            }

            Map<String, Object> qm = new HashMap<>();
            qm.put("questionId", q.getId());
            qm.put("content", q.getContent());
            qm.put("questionType", q.getQuestionType());
            qm.put("standardAnswer", q.getStandardAnswer());
            qm.put("options", q.getOptions());
            qm.put("fullScore", pq.getScore());
            qm.put("objective", objective);
            qm.put("optionDist", optionDist);
            qm.put("stat", Map.of(
                    "total", students.size(),
                    "submitted", submitted,
                    "answered", answered,
                    "correct", correctCnt,
                    "avgScore", answered == 0 ? 0 :
                            scoreSum.divide(BigDecimal.valueOf(answered), 2, RoundingMode.HALF_UP).doubleValue()));
            qm.put("rows", rows);
            questions.add(qm);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("paperId", paperId);
        result.put("paperName", paper.getPaperName());
        result.put("classId", classId);
        result.put("studentCount", students.size());
        result.put("submittedCount", answerByStudent.size());
        result.put("questions", questions);
        return result;
    }

    /** 批量批改试卷所有已提交作答 */
    /**
     * 批量复核(AI 自检):学生提交时已自动 AI 初批,这里由 AI 对主观题再做一次独立批改,
     * 与初批结论比对:不一致的题标记「存疑」(review_status=1)待教师人工确认,
     * 一致的清除存疑标记;教师已手动微调(MANUAL)的记录不动;客观/填空题为确定性批改,跳过
     */
    @Transactional
    public Map<String, Object> batchCorrectPaper(Long paperId) {
        List<ExamAnswer> answers = answerMapper.selectByPaperId(paperId);
        int reviewed = 0, skip = 0, diffCount = 0;
        for (ExamAnswer a : answers) {
            if (a.getStatus() == null || a.getStatus() < SystemConstants.ANSWER_STATUS_SUBMITTED) {
                skip++;
                continue;
            }
            List<AiCorrectRecord> records = correctMapper.selectByAnswerId(a.getId());
            if (records.isEmpty()) {
                // 从未批改过(自动初批未执行),补一次完整初批
                correctAnswer(a.getId(), new HashMap<>());
                reviewed++;
                continue;
            }
            boolean changed = false;
            for (AiCorrectRecord r : records) {
                // 教师手动微调过的记录,保留不动
                if (r.getCorrectType() != null && r.getCorrectType() == SystemConstants.CORRECT_TYPE_MANUAL) continue;
                ExamQuestion q = questionMapper.selectById(r.getQuestionId());
                if (q == null) continue;
                Integer type = q.getQuestionType();
                // 客观/填空题由本地规则确定性批改,自检无意义,只复核简答/作文/计算
                if (type == null || type < SystemConstants.Q_TYPE_SHORT) continue;
                String verdict = aiServiceProvider.correctVariant(q.getContent(), q.getStandardAnswer(), r.getStudentAnswer());
                if (verdict == null) continue; // AI 服务不可用,保留初批结果
                boolean aiRight = verdict.startsWith("正确");
                boolean oldRight = r.getIsCorrect() != null && r.getIsCorrect() == 1;
                // 去掉上一轮复核意见,避免重复堆积
                String baseRemark = r.getCorrectRemark() == null ? ""
                        : r.getCorrectRemark().replaceAll("AI复核:[^\n]*\n?", "").trim();
                if (aiRight != oldRight) {
                    r.setReviewStatus(1); // 存疑,待教师确认
                    diffCount++;
                } else {
                    r.setReviewStatus(0);
                }
                r.setCorrectRemark((baseRemark.isEmpty() ? "" : baseRemark + "\n")
                        + "AI复核:" + verdict.replace("\n", " ").trim());
                correctMapper.updateById(r);
                changed = true;
            }
            if (changed) {
                ExamAnswer ua = new ExamAnswer();
                ua.setId(a.getId());
                ua.setStatus(SystemConstants.ANSWER_STATUS_REVIEWED);
                answerMapper.updateById(ua);
                reviewed++;
            } else {
                skip++;
            }
        }
        return Map.of("total", answers.size(), "reviewed", reviewed, "skip", skip, "diff", diffCount);
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
        // 该生尚无学情分析时,现场自动生成(有作答数据才会产生结果)
        if (sa == null) {
            sa = analysisEngine.analyzeStudent(studentId, null);
        }
        String perf = sa == null ? "" : "平均分:" + sa.getAvgScore() +
                ",薄弱点:" + (sa.getWeakPoints() == null ? "无" : sa.getWeakPoints()) +
                ",优势:" + (sa.getStrongPoints() == null ? "无" : sa.getStrongPoints());
        return aiServiceProvider.generateComment(u == null ? null : u.getRealName(), perf);
    }

    // ===================== AI 助学 =====================

    /** AI 答疑(文字/拍照/语音) */
    @Transactional
    public Map<String, Object> tutorChat(Long studentId, String question, Integer chatType, String imageBase64) {
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
