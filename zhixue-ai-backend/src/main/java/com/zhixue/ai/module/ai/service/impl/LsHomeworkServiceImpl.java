package com.zhixue.ai.module.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.module.ai.engine.AiServiceProvider;
import com.zhixue.ai.module.ai.entity.AiLsHomework;
import com.zhixue.ai.module.ai.entity.AiLsHomeworkQuestion;
import com.zhixue.ai.module.ai.entity.AiLsHomeworkRecord;
import com.zhixue.ai.module.ai.entity.AiListeningSpeaking;
import com.zhixue.ai.module.ai.mapper.AiLsHomeworkMapper;
import com.zhixue.ai.module.ai.mapper.AiLsHomeworkQuestionMapper;
import com.zhixue.ai.module.ai.mapper.AiLsHomeworkRecordMapper;
import com.zhixue.ai.module.ai.mapper.AiListeningSpeakingMapper;
import com.zhixue.ai.module.ai.service.LsHomeworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 教师英语听说作业服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LsHomeworkServiceImpl implements LsHomeworkService {

    private final AiLsHomeworkMapper homeworkMapper;
    private final AiLsHomeworkQuestionMapper questionMapper;
    private final AiLsHomeworkRecordMapper recordMapper;
    private final AiListeningSpeakingMapper lsMapper;
    private final AiServiceProvider aiServiceProvider;

    private static final List<String> AUDIO_TYPES = Arrays.asList("mp3", "wav", "m4a");
    private static final long MAX_AUDIO_SIZE = 10 * 1024 * 1024L;

    @Override
    public Long createHomework(Long teacherId, AiLsHomework homework) {
        homework.setTeacherId(teacherId);
        homework.setStatus(0); // 草稿
        homeworkMapper.insert(homework);
        return homework.getId();
    }

    @Override
    public List<Map<String, Object>> generateQuestions(String mode, String params) {
        String resultJson = aiServiceProvider.generateLsHomework(mode, params);
        if (resultJson == null) {
            throw new BizException("AI 组题失败，请检查 AI 服务配置或稍后重试");
        }
        try {
            String cleanJson = resultJson;
            int start = cleanJson.indexOf('[');
            int end = cleanJson.lastIndexOf(']');
            if (start >= 0 && end > start) {
                cleanJson = cleanJson.substring(start, end + 1);
            }
            JSONArray arr = JSON.parseArray(cleanJson);
            List<Map<String, Object>> questions = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                Map<String, Object> q = new HashMap<>();
                q.put("title", obj.getString("title"));
                q.put("content", obj.getString("content"));
                q.put("referenceText", obj.getString("referenceText"));
                q.put("questionType", obj.getString("questionType"));
                q.put("difficulty", obj.getInteger("difficulty") != null ? obj.getInteger("difficulty") : 2);
                q.put("scorePoints", obj.getString("scorePoints"));
                questions.add(q);
            }
            return questions;
        } catch (Exception e) {
            log.error("解析 AI 组题结果失败: {}", resultJson, e);
            throw new BizException("AI 组题结果格式异常，请重试");
        }
    }

    @Override
    public void saveQuestions(Long homeworkId, List<AiLsHomeworkQuestion> questions) {
        for (int i = 0; i < questions.size(); i++) {
            AiLsHomeworkQuestion q = questions.get(i);
            q.setHomeworkId(homeworkId);
            q.setSortOrder(i);
            questionMapper.insert(q);
        }
    }

    @Override
    public Map<String, Object> getHomeworkDetail(Long homeworkId) {
        AiLsHomework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null) {
            throw new BizException("作业不存在");
        }
        List<AiLsHomeworkQuestion> questions = questionMapper.selectByHomeworkId(homeworkId);
        Map<String, Object> result = new HashMap<>();
        result.put("id", homework.getId());
        result.put("title", homework.getTitle());
        result.put("classId", homework.getClassId());
        result.put("gradeLevel", homework.getGradeLevel());
        result.put("groupMode", homework.getGroupMode());
        result.put("deadline", homework.getDeadline());
        result.put("status", homework.getStatus());
        result.put("questions", questions);
        return result;
    }

    @Override
    public void publishHomework(Long homeworkId, Long teacherId) {
        AiLsHomework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null || !homework.getTeacherId().equals(teacherId)) {
            throw new BizException("作业不存在或无权限");
        }
        homework.setStatus(1); // 已发布
        homeworkMapper.updateById(homework);
    }

    @Override
    public List<Map<String, Object>> listTeacherHomework(Long teacherId) {
        List<AiLsHomework> list = homeworkMapper.selectList(
                new LambdaQueryWrapper<AiLsHomework>()
                        .eq(AiLsHomework::getTeacherId, teacherId)
                        .orderByDesc(AiLsHomework::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiLsHomework h : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", h.getId());
            item.put("title", h.getTitle());
            item.put("classId", h.getClassId());
            item.put("gradeLevel", h.getGradeLevel());
            item.put("groupMode", h.getGroupMode());
            item.put("deadline", h.getDeadline());
            item.put("status", h.getStatus());
            item.put("createTime", h.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> listStudentHomework(Long studentId) {
        // 简化实现：返回所有已发布的作业（实际应根据班级过滤）
        List<AiLsHomework> list = homeworkMapper.selectList(
                new LambdaQueryWrapper<AiLsHomework>()
                        .eq(AiLsHomework::getStatus, 1)
                        .orderByDesc(AiLsHomework::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiLsHomework h : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", h.getId());
            item.put("title", h.getTitle());
            item.put("deadline", h.getDeadline());
            item.put("createTime", h.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> submitHomeworkAnswer(Long studentId, Long questionId, MultipartFile audioFile, String supplementText) {
        AiLsHomeworkQuestion question = questionMapper.selectById(questionId);
        if (question == null) {
            throw new BizException("题目不存在");
        }

        // 保存音频
        String audioPath = saveAudioFile(audioFile);
        File savedFile = new File(new File("upload").getAbsoluteFile(),
                audioPath.replace("/upload/", ""));

        try {
            byte[] audioBytes = java.nio.file.Files.readAllBytes(savedFile.toPath());
            String format = getAudioFormat(audioFile.getOriginalFilename());
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            String recognizedText = aiServiceProvider.recognizeAudio(base64Audio, format);

            AiLsHomeworkRecord record = new AiLsHomeworkRecord();
            record.setHomeworkId(question.getHomeworkId());
            record.setQuestionId(questionId);
            record.setStudentId(studentId);
            record.setAudioPath(audioPath);
            record.setSupplementText(supplementText);
            record.setRecognizedText(recognizedText);

            if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                String gradeJson = aiServiceProvider.gradeListeningSpeaking(
                        recognizedText, question.getReferenceText(), question.getContent());
                fillScores(record, gradeJson);
            } else {
                record.setPronunciationScore(BigDecimal.ZERO);
                record.setFluencyScore(BigDecimal.ZERO);
                record.setGrammarScore(BigDecimal.ZERO);
                record.setContentScore(BigDecimal.ZERO);
                record.setTotalScore(BigDecimal.ZERO);
                record.setAiFeedback("语音识别失败");
            }

            recordMapper.insert(record);

            Map<String, Object> result = new HashMap<>();
            result.put("id", record.getId());
            result.put("recognizedText", record.getRecognizedText());
            result.put("pronunciationScore", record.getPronunciationScore());
            result.put("fluencyScore", record.getFluencyScore());
            result.put("grammarScore", record.getGrammarScore());
            result.put("contentScore", record.getContentScore());
            result.put("totalScore", record.getTotalScore());
            result.put("aiFeedback", record.getAiFeedback());
            return result;
        } catch (IOException e) {
            log.error("读取音频文件失败: {}", e.getMessage(), e);
            throw new BizException("读取音频文件失败");
        }
    }

    @Override
    public List<Map<String, Object>> listStudentHomeworkRecords(Long studentId, Long homeworkId) {
        List<AiLsHomeworkRecord> records = recordMapper.selectByHomeworkAndStudent(homeworkId, studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiLsHomeworkRecord r : records) {
            AiLsHomeworkQuestion q = questionMapper.selectById(r.getQuestionId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("questionId", r.getQuestionId());
            item.put("questionTitle", q == null ? "题目已删除" : q.getTitle());
            item.put("recognizedText", r.getRecognizedText());
            item.put("pronunciationScore", r.getPronunciationScore());
            item.put("fluencyScore", r.getFluencyScore());
            item.put("grammarScore", r.getGrammarScore());
            item.put("contentScore", r.getContentScore());
            item.put("totalScore", r.getTotalScore());
            item.put("aiFeedback", r.getAiFeedback());
            item.put("createTime", r.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getClassHomeworkReport(Long homeworkId, Long teacherId) {
        AiLsHomework homework = homeworkMapper.selectById(homeworkId);
        if (homework == null || !homework.getTeacherId().equals(teacherId)) {
            throw new BizException("作业不存在或无权限");
        }
        List<AiLsHomeworkRecord> records = recordMapper.selectByHomeworkId(homeworkId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiLsHomeworkRecord r : records) {
            AiLsHomeworkQuestion q = questionMapper.selectById(r.getQuestionId());
            Map<String, Object> item = new HashMap<>();
            item.put("studentId", r.getStudentId());
            item.put("questionId", r.getQuestionId());
            item.put("questionTitle", q == null ? "题目已删除" : q.getTitle());
            item.put("totalScore", r.getTotalScore());
            item.put("aiFeedback", r.getAiFeedback());
            item.put("createTime", r.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public Long copyHomework(Long homeworkId, Long teacherId, boolean regenerate) {
        AiLsHomework original = homeworkMapper.selectById(homeworkId);
        if (original == null) {
            throw new BizException("作业不存在");
        }
        AiLsHomework copy = new AiLsHomework();
        copy.setTitle(original.getTitle() + " (副本)");
        copy.setClassId(original.getClassId());
        copy.setGradeLevel(original.getGradeLevel());
        copy.setGroupMode(original.getGroupMode());
        copy.setGroupParams(original.getGroupParams());
        copy.setDeadline(original.getDeadline());
        copy.setStatus(0); // 草稿
        homeworkMapper.insert(copy);

        if (!regenerate) {
            // 复制题目
            List<AiLsHomeworkQuestion> questions = questionMapper.selectByHomeworkId(homeworkId);
            for (AiLsHomeworkQuestion q : questions) {
                AiLsHomeworkQuestion copyQ = new AiLsHomeworkQuestion();
                copyQ.setHomeworkId(copy.getId());
                copyQ.setTitle(q.getTitle());
                copyQ.setContent(q.getContent());
                copyQ.setReferenceText(q.getReferenceText());
                copyQ.setReferenceAudio(q.getReferenceAudio());
                copyQ.setQuestionType(q.getQuestionType());
                copyQ.setDifficulty(q.getDifficulty());
                copyQ.setScorePoints(q.getScorePoints());
                copyQ.setSortOrder(q.getSortOrder());
                questionMapper.insert(copyQ);
            }
        }

        return copy.getId();
    }

    private void fillScores(AiLsHomeworkRecord record, String gradeJson) {
        if (gradeJson == null || gradeJson.trim().isEmpty()) {
            record.setPronunciationScore(BigDecimal.ZERO);
            record.setFluencyScore(BigDecimal.ZERO);
            record.setGrammarScore(BigDecimal.ZERO);
            record.setContentScore(BigDecimal.ZERO);
            record.setTotalScore(BigDecimal.ZERO);
            record.setAiFeedback("AI 评分失败");
            return;
        }
        try {
            String json = gradeJson;
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JSONObject obj = JSON.parseObject(json);
            BigDecimal pronunciation = obj.getBigDecimal("pronunciationScore");
            BigDecimal fluency = obj.getBigDecimal("fluencyScore");
            BigDecimal grammar = obj.getBigDecimal("grammarScore");
            BigDecimal content = obj.getBigDecimal("contentScore");
            if (pronunciation == null || fluency == null || grammar == null || content == null) {
                throw new IllegalArgumentException("评分字段缺失");
            }
            record.setPronunciationScore(pronunciation);
            record.setFluencyScore(fluency);
            record.setGrammarScore(grammar);
            record.setContentScore(content);
            record.setTotalScore(pronunciation.add(fluency).add(grammar).add(content));
            record.setAiFeedback(obj.getString("feedback"));
        } catch (Exception e) {
            log.error("解析 AI 评分结果失败: {}", gradeJson, e);
            record.setPronunciationScore(BigDecimal.ZERO);
            record.setFluencyScore(BigDecimal.ZERO);
            record.setGrammarScore(BigDecimal.ZERO);
            record.setContentScore(BigDecimal.ZERO);
            record.setTotalScore(BigDecimal.ZERO);
            record.setAiFeedback("AI 评分结果解析失败");
        }
    }

    private String saveAudioFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("音频文件不能为空");
        }
        if (file.getSize() > MAX_AUDIO_SIZE) {
            throw new BizException("音频文件不能超过 10MB");
        }
        String originalName = file.getOriginalFilename();
        String suffix = originalName == null ? "" :
                originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!AUDIO_TYPES.contains(suffix)) {
            throw new BizException("不支持的音频格式: " + suffix);
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        File uploadRoot = new File("upload").getAbsoluteFile();
        File dir = new File(uploadRoot, "audio" + File.separator + datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("创建音频目录失败");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            throw new BizException("音频上传失败");
        }
        return "/upload/audio/" + datePath + "/" + fileName;
    }

    private String getAudioFormat(String originalName) {
        if (originalName == null) {
            return "wav";
        }
        String suffix = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        return AUDIO_TYPES.contains(suffix) ? suffix : "wav";
    }
}
