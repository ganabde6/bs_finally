package com.zhixue.ai.module.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.module.ai.engine.AiServiceProvider;
import com.zhixue.ai.module.ai.entity.AiListeningSpeaking;
import com.zhixue.ai.module.ai.entity.AiListeningSpeakingRecord;
import com.zhixue.ai.module.ai.mapper.AiListeningSpeakingMapper;
import com.zhixue.ai.module.ai.mapper.AiListeningSpeakingRecordMapper;
import com.zhixue.ai.module.ai.service.ListeningSpeakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 高考英语听说练习服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ListeningSpeakingServiceImpl implements ListeningSpeakingService {

    private final AiListeningSpeakingMapper listeningSpeakingMapper;
    private final AiListeningSpeakingRecordMapper recordMapper;
    private final AiServiceProvider aiServiceProvider;

    /** 音频文件允许的类型 */
    private static final List<String> AUDIO_TYPES = Arrays.asList("mp3", "wav", "m4a");
    /** 音频文件大小上限(10MB) */
    private static final long MAX_AUDIO_SIZE = 10 * 1024 * 1024L;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public List<AiListeningSpeaking> listQuestions(Integer gradeLevel) {
        LambdaQueryWrapper<AiListeningSpeaking> wrapper = new LambdaQueryWrapper<AiListeningSpeaking>()
                .eq(AiListeningSpeaking::getStatus, 1)
                .orderByAsc(AiListeningSpeaking::getDifficulty)
                .orderByDesc(AiListeningSpeaking::getCreateTime);
        // 学段过滤:指定学段时返回「该学段 + 通用」题目
        if (gradeLevel != null && gradeLevel > 0) {
            wrapper.and(w -> w.eq(AiListeningSpeaking::getGradeLevel, gradeLevel)
                    .or().eq(AiListeningSpeaking::getGradeLevel, 0));
        }
        return listeningSpeakingMapper.selectList(wrapper);
    }

    @Override
    public AiListeningSpeaking getQuestionDetail(Long id) {
        AiListeningSpeaking question = listeningSpeakingMapper.selectById(id);
        if (question == null) {
            throw new BizException("题目不存在");
        }
        return question;
    }

    @Override
    public Map<String, Object> submitAnswer(Long userId, Long questionId, MultipartFile audioFile, String supplementText) {
        AiListeningSpeaking question = getQuestionDetail(questionId);

        // 1. 保存音频文件
        String audioPath = saveAudioFile(audioFile);
        // 与保存时一致,使用绝对路径定位已保存的音频文件
        File savedFile = new File(new File(uploadDir).getAbsoluteFile(),
                audioPath.replace("/upload/", ""));

        try {
            // 2. 读取音频字节并转 base64 调用 qwen-audio-turbo 语音识别
            byte[] audioBytes = java.nio.file.Files.readAllBytes(savedFile.toPath());
            String format = getAudioFormat(audioFile.getOriginalFilename());
            String base64Audio = Base64.getEncoder().encodeToString(audioBytes);
            String recognizedText = aiServiceProvider.recognizeAudio(base64Audio, format);

            AiListeningSpeakingRecord record = new AiListeningSpeakingRecord();
            record.setUserId(userId);
            record.setQuestionId(questionId);
            record.setAudioPath(audioPath);
            record.setSupplementText(supplementText);
            record.setRecognizedText(recognizedText);

            if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                // 3. 调用文本模型评分
                String gradeJson = aiServiceProvider.gradeListeningSpeaking(
                        recognizedText, question.getReferenceText(), question.getContent());
                fillScores(record, gradeJson);
            } else {
                // 识别失败(未配置 API 或音频为空)
                record.setPronunciationScore(BigDecimal.ZERO);
                record.setFluencyScore(BigDecimal.ZERO);
                record.setGrammarScore(BigDecimal.ZERO);
                record.setContentScore(BigDecimal.ZERO);
                record.setTotalScore(BigDecimal.ZERO);
                record.setAiFeedback("语音识别失败:请确认已配置通义千问 API Key 且录音清晰可辨,然后重新作答。");
            }

            // 4. 保存作答记录
            recordMapper.insert(record);

            // 5. 组装返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("id", record.getId());
            result.put("questionId", questionId);
            result.put("audioPath", audioPath);
            result.put("recognizedText", record.getRecognizedText());
            result.put("pronunciationScore", record.getPronunciationScore());
            result.put("fluencyScore", record.getFluencyScore());
            result.put("grammarScore", record.getGrammarScore());
            result.put("contentScore", record.getContentScore());
            result.put("totalScore", record.getTotalScore());
            result.put("aiFeedback", record.getAiFeedback());
            result.put("createTime", record.getCreateTime());
            return result;
        } catch (IOException e) {
            log.error("读取音频文件失败: {}", e.getMessage(), e);
            throw new BizException("读取音频文件失败: " + e.getMessage());
        }
    }

    /**
     * 解析 AI 评分 JSON 并填充到作答记录
     */
    private void fillScores(AiListeningSpeakingRecord record, String gradeJson) {
        if (gradeJson == null || gradeJson.trim().isEmpty()) {
            record.setPronunciationScore(BigDecimal.ZERO);
            record.setFluencyScore(BigDecimal.ZERO);
            record.setGrammarScore(BigDecimal.ZERO);
            record.setContentScore(BigDecimal.ZERO);
            record.setTotalScore(BigDecimal.ZERO);
            record.setAiFeedback("AI 评分失败,请稍后重试或检查 AI 服务配置。");
            return;
        }
        try {
            String json = gradeJson;
            // 兼容 AI 返回内容中可能包含的代码块标记
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
            record.setAiFeedback("AI 评分结果解析失败,请重试。");
        }
    }

    @Override
    public List<Map<String, Object>> listMyRecords(Long userId) {
        List<AiListeningSpeakingRecord> records = recordMapper.selectList(
                new LambdaQueryWrapper<AiListeningSpeakingRecord>()
                        .eq(AiListeningSpeakingRecord::getUserId, userId)
                        .orderByDesc(AiListeningSpeakingRecord::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiListeningSpeakingRecord r : records) {
            AiListeningSpeaking question = listeningSpeakingMapper.selectById(r.getQuestionId());
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("questionId", r.getQuestionId());
            item.put("questionTitle", question == null ? "题目已删除" : question.getTitle());
            item.put("questionType", question == null ? "" : question.getQuestionType());
            item.put("audioPath", r.getAudioPath());
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
    public String uploadAudio(MultipartFile file) {
        return saveAudioFile(file);
    }

    /**
     * 保存音频文件到 upload/audio/ 目录,返回 /upload/audio/yyyy/MM/dd/xxx.ext 相对路径
     */
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
            throw new BizException("不支持的音频格式: " + suffix + "(仅支持 mp3/wav/m4a)");
        }
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        // 必须使用绝对路径:Tomcat 的 MultipartFile.transferTo 对相对路径会解析到
        // multipart 临时目录(上下文 tempdir),而不是 JVM 工作目录,导致文件写错位置
        File uploadRoot = new File(uploadDir).getAbsoluteFile();
        File dir = new File(uploadRoot, "audio" + File.separator + datePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BizException("创建音频目录失败");
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + suffix;
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            throw new BizException("音频上传失败: " + e.getMessage());
        }
        return "/upload/audio/" + datePath + "/" + fileName;
    }

    /**
     * 从文件名提取音频格式(默认 wav)
     */
    private String getAudioFormat(String originalName) {
        if (originalName == null) {
            return "wav";
        }
        String suffix = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        return AUDIO_TYPES.contains(suffix) ? suffix : "wav";
    }

    // ===================== 学生自主出题 =====================

    @Override
    public Map<String, Object> generateFromText(Long userId, String text, String questionType, Integer gradeLevel) {
        if (text == null || text.trim().isEmpty()) {
            throw new BizException("请输入英文文本");
        }
        String resultJson = aiServiceProvider.generateLsFromText(text, questionType, gradeLevel);
        if (resultJson == null) {
            throw new BizException("AI 生成题目失败，请检查 AI 服务配置或稍后重试");
        }
        AiListeningSpeaking question = parseAndSaveQuestion(resultJson, userId, "AI_TEXT", null, null);
        return questionToMap(question);
    }

    @Override
    public Map<String, Object> generateFromTopic(Long userId, String topic, String questionType, Integer difficulty, Integer gradeLevel) {
        String resultJson = aiServiceProvider.generateLsFromTopic(topic, questionType, difficulty, gradeLevel);
        if (resultJson == null) {
            throw new BizException("AI 生成题目失败，请检查 AI 服务配置或稍后重试");
        }
        AiListeningSpeaking question = parseAndSaveQuestion(resultJson, userId, "AI_TOPIC", topic, difficulty);
        return questionToMap(question);
    }

    @Override
    public Map<String, Object> generateFromImage(Long userId, String imageBase64, String questionType, Integer gradeLevel) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            throw new BizException("请上传图片");
        }
        String resultJson = aiServiceProvider.generateLsFromImage(imageBase64, questionType, gradeLevel);
        if (resultJson == null) {
            throw new BizException("AI 生成题目失败，请检查 AI 服务配置或稍后重试");
        }
        AiListeningSpeaking question = parseAndSaveQuestion(resultJson, userId, "AI_IMAGE", null, null);
        question.setImageUrl("data:image/png;base64," + imageBase64.substring(0, Math.min(100, imageBase64.length())) + "...");
        listeningSpeakingMapper.updateById(question);
        return questionToMap(question);
    }

    @Override
    public Map<String, Object> generateSimilar(Long userId, Long previousQuestionId) {
        AiListeningSpeaking prev = listeningSpeakingMapper.selectById(previousQuestionId);
        if (prev == null) {
            throw new BizException("上一题不存在");
        }
        String resultJson = aiServiceProvider.generateSimilarLs(
                prev.getContent(), prev.getQuestionType(), prev.getTopic(), prev.getGradeLevel());
        if (resultJson == null) {
            throw new BizException("AI 生成同类题目失败，请检查 AI 服务配置或稍后重试");
        }
        AiListeningSpeaking question = parseAndSaveQuestion(resultJson, userId, "AI_SIMILAR", prev.getTopic(), prev.getDifficulty());
        return questionToMap(question);
    }

    @Override
    public List<String> getTopics(Integer gradeLevel) {
        LambdaQueryWrapper<AiListeningSpeaking> wrapper = new LambdaQueryWrapper<AiListeningSpeaking>()
                .eq(AiListeningSpeaking::getStatus, 1)
                .isNotNull(AiListeningSpeaking::getTopic)
                .ne(AiListeningSpeaking::getTopic, "");
        if (gradeLevel != null && gradeLevel > 0) {
            wrapper.and(w -> w.eq(AiListeningSpeaking::getGradeLevel, gradeLevel)
                    .or().eq(AiListeningSpeaking::getGradeLevel, 0));
        }
        List<AiListeningSpeaking> questions = listeningSpeakingMapper.selectList(wrapper);
        return questions.stream()
                .map(AiListeningSpeaking::getTopic)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 解析 AI 返回的 JSON 并保存为题目
     */
    private AiListeningSpeaking parseAndSaveQuestion(String json, Long userId, String sourceType, String topic, Integer difficulty) {
        try {
            String cleanJson = json;
            int start = cleanJson.indexOf('{');
            int end = cleanJson.lastIndexOf('}');
            if (start >= 0 && end > start) {
                cleanJson = cleanJson.substring(start, end + 1);
            }
            JSONObject obj = JSON.parseObject(cleanJson);
            AiListeningSpeaking q = new AiListeningSpeaking();
            q.setTitle(obj.getString("title"));
            q.setContent(obj.getString("content"));
            q.setReferenceText(obj.getString("referenceText"));
            q.setQuestionType(obj.getString("questionType"));
            q.setDifficulty(obj.getInteger("difficulty") != null ? obj.getInteger("difficulty") : (difficulty != null ? difficulty : 2));
            q.setGradeLevel(0); // 自主出题通用
            q.setTopic(topic);
            q.setSourceType(sourceType);
            q.setStudentId(userId);
            q.setScorePoints(obj.getString("scorePoints"));
            q.setStatus(1);
            listeningSpeakingMapper.insert(q);
            return q;
        } catch (Exception e) {
            log.error("解析 AI 生成题目失败: {}", json, e);
            throw new BizException("AI 生成题目格式异常，请重试");
        }
    }

    /**
     * 将题目转为 Map 返回
     */
    private Map<String, Object> questionToMap(AiListeningSpeaking q) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", q.getId());
        map.put("title", q.getTitle());
        map.put("content", q.getContent());
        map.put("referenceText", q.getReferenceText());
        map.put("questionType", q.getQuestionType());
        map.put("difficulty", q.getDifficulty());
        map.put("topic", q.getTopic());
        map.put("sourceType", q.getSourceType());
        map.put("scorePoints", q.getScorePoints());
        map.put("gradeLevel", q.getGradeLevel());
        return map;
    }
}
