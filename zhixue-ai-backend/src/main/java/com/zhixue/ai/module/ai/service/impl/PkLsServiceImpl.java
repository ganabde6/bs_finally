package com.zhixue.ai.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.module.ai.engine.AiServiceProvider;
import com.zhixue.ai.module.ai.entity.PkLsRecord;
import com.zhixue.ai.module.ai.entity.PkLsRoom;
import com.zhixue.ai.module.ai.mapper.PkLsRecordMapper;
import com.zhixue.ai.module.ai.mapper.PkLsRoomMapper;
import com.zhixue.ai.module.ai.service.PkLsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 英语听说 PK 服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PkLsServiceImpl implements PkLsService {

    private final PkLsRoomMapper roomMapper;
    private final PkLsRecordMapper recordMapper;
    private final AiServiceProvider aiServiceProvider;

    private static final List<String> AUDIO_TYPES = Arrays.asList("mp3", "wav", "m4a");
    private static final long MAX_AUDIO_SIZE = 10 * 1024 * 1024L;

    @Override
    public Map<String, Object> createChallenge(Long creatorId, String questionTitle, String questionContent,
                                                String referenceText, String questionType, String scorePoints) {
        if (questionContent == null || questionContent.trim().isEmpty()) {
            throw new BizException("题目内容不能为空");
        }

        PkLsRoom room = new PkLsRoom();
        room.setRoomCode(generateRoomCode());
        room.setCreatorId(creatorId);
        room.setQuestionTitle(questionTitle);
        room.setQuestionContent(questionContent);
        room.setReferenceText(referenceText);
        room.setQuestionType(questionType);
        room.setScorePoints(scorePoints);
        room.setStatus(0); // 等待挑战
        room.setCreateTime(LocalDateTime.now());
        roomMapper.insert(room);

        Map<String, Object> result = new HashMap<>();
        result.put("roomCode", room.getRoomCode());
        result.put("questionTitle", room.getQuestionTitle());
        result.put("questionType", room.getQuestionType());
        result.put("status", "waiting");
        return result;
    }

    @Override
    public Map<String, Object> acceptChallenge(Long challengerId, String roomCode) {
        PkLsRoom room = roomMapper.selectByRoomCode(roomCode);
        if (room == null) {
            throw new BizException("挑战不存在");
        }
        if (room.getStatus() != 0) {
            throw new BizException("该挑战已被接受或已完成");
        }
        if (room.getCreatorId().equals(challengerId)) {
            throw new BizException("不能接受自己的挑战");
        }

        room.setChallengerId(challengerId);
        room.setStatus(1); // 已接受
        room.setUpdateTime(LocalDateTime.now());
        roomMapper.updateById(room);

        Map<String, Object> result = new HashMap<>();
        result.put("roomCode", room.getRoomCode());
        result.put("questionTitle", room.getQuestionTitle());
        result.put("questionContent", room.getQuestionContent());
        result.put("referenceText", room.getReferenceText());
        result.put("questionType", room.getQuestionType());
        result.put("scorePoints", room.getScorePoints());
        result.put("status", "accepted");
        return result;
    }

    @Override
    public Map<String, Object> getChallengeDetail(String roomCode) {
        PkLsRoom room = roomMapper.selectByRoomCode(roomCode);
        if (room == null) {
            throw new BizException("挑战不存在");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("roomCode", room.getRoomCode());
        result.put("creatorId", room.getCreatorId());
        result.put("challengerId", room.getChallengerId());
        result.put("questionTitle", room.getQuestionTitle());
        result.put("questionContent", room.getQuestionContent());
        result.put("referenceText", room.getReferenceText());
        result.put("questionType", room.getQuestionType());
        result.put("scorePoints", room.getScorePoints());
        result.put("status", room.getStatus());

        // 获取双方作答记录
        List<PkLsRecord> records = recordMapper.selectByRoomId(room.getId());
        List<Map<String, Object>> recordList = new ArrayList<>();
        for (PkLsRecord r : records) {
            Map<String, Object> rec = new HashMap<>();
            rec.put("studentId", r.getStudentId());
            rec.put("audioPath", r.getAudioPath());
            rec.put("recognizedText", r.getRecognizedText());
            rec.put("pronunciationScore", r.getPronunciationScore());
            rec.put("fluencyScore", r.getFluencyScore());
            rec.put("grammarScore", r.getGrammarScore());
            rec.put("contentScore", r.getContentScore());
            rec.put("totalScore", r.getTotalScore());
            rec.put("aiFeedback", r.getAiFeedback());
            recordList.add(rec);
        }
        result.put("records", recordList);

        return result;
    }

    @Override
    public Map<String, Object> submitPkAnswer(Long studentId, String roomCode, MultipartFile audioFile, String supplementText) {
        PkLsRoom room = roomMapper.selectByRoomCode(roomCode);
        if (room == null) {
            throw new BizException("挑战不存在");
        }
        if (room.getStatus() < 1) {
            throw new BizException("挑战尚未被接受");
        }

        // 检查是否已作答
        List<PkLsRecord> existingRecords = recordMapper.selectByRoomId(room.getId());
        for (PkLsRecord r : existingRecords) {
            if (r.getStudentId().equals(studentId)) {
                throw new BizException("你已经作答过了");
            }
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
            // 语音识别失败时,若学生提供了补充文本,则用补充文本参与评分
            boolean recognitionFailed = recognizedText == null || recognizedText.trim().isEmpty();
            if (recognitionFailed && supplementText != null && !supplementText.trim().isEmpty()) {
                recognizedText = supplementText.trim();
            }

            PkLsRecord record = new PkLsRecord();
            record.setRoomId(room.getId());
            record.setStudentId(studentId);
            record.setAudioPath(audioPath);
            record.setRecognizedText(recognizedText);

            if (recognizedText != null && !recognizedText.trim().isEmpty()) {
                String gradeJson = aiServiceProvider.gradeListeningSpeaking(
                        recognizedText, room.getReferenceText(), room.getQuestionContent());
                fillScores(record, gradeJson);
                // 识别失败但使用补充文本评分时,在评语中标注来源
                if (recognitionFailed) {
                    String feedback = record.getAiFeedback();
                    record.setAiFeedback("语音识别失败,已使用补充文本评分"
                            + (feedback == null || feedback.isEmpty() ? "" : "; " + feedback));
                }
            } else {
                record.setPronunciationScore(BigDecimal.ZERO);
                record.setFluencyScore(BigDecimal.ZERO);
                record.setGrammarScore(BigDecimal.ZERO);
                record.setContentScore(BigDecimal.ZERO);
                record.setTotalScore(BigDecimal.ZERO);
                record.setAiFeedback("语音识别失败");
            }

            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);

            // 检查双方是否都已完成
            List<PkLsRecord> allRecords = recordMapper.selectByRoomId(room.getId());
            if (allRecords.size() >= 2) {
                room.setStatus(2); // 已完成
                room.setUpdateTime(LocalDateTime.now());
                roomMapper.updateById(room);
            }

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
    public Map<String, Object> getPkResult(String roomCode) {
        PkLsRoom room = roomMapper.selectByRoomCode(roomCode);
        if (room == null) {
            throw new BizException("挑战不存在");
        }

        List<PkLsRecord> records = recordMapper.selectByRoomId(room.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("roomCode", room.getRoomCode());
        result.put("questionTitle", room.getQuestionTitle());
        result.put("status", room.getStatus());

        List<Map<String, Object>> playerResults = new ArrayList<>();
        for (PkLsRecord r : records) {
            Map<String, Object> player = new HashMap<>();
            player.put("studentId", r.getStudentId());
            player.put("totalScore", r.getTotalScore());
            player.put("pronunciationScore", r.getPronunciationScore());
            player.put("fluencyScore", r.getFluencyScore());
            player.put("grammarScore", r.getGrammarScore());
            player.put("contentScore", r.getContentScore());
            player.put("aiFeedback", r.getAiFeedback());
            playerResults.add(player);
        }

        // 按总分排序
        playerResults.sort((a, b) -> {
            BigDecimal scoreA = (BigDecimal) a.get("totalScore");
            BigDecimal scoreB = (BigDecimal) b.get("totalScore");
            return scoreB.compareTo(scoreA);
        });

        // 添加排名
        for (int i = 0; i < playerResults.size(); i++) {
            playerResults.get(i).put("rank", i + 1);
        }

        result.put("players", playerResults);
        return result;
    }

    @Override
    public List<Map<String, Object>> listMyChallenges(Long userId) {
        List<PkLsRoom> rooms = roomMapper.selectList(
                new LambdaQueryWrapper<PkLsRoom>()
                        .and(w -> w.eq(PkLsRoom::getCreatorId, userId)
                                .or().eq(PkLsRoom::getChallengerId, userId))
                        .orderByDesc(PkLsRoom::getCreateTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (PkLsRoom room : rooms) {
            Map<String, Object> item = new HashMap<>();
            item.put("roomCode", room.getRoomCode());
            item.put("questionTitle", room.getQuestionTitle());
            item.put("questionType", room.getQuestionType());
            item.put("status", room.getStatus());
            item.put("createTime", room.getCreateTime());
            item.put("isCreator", room.getCreatorId().equals(userId));
            result.add(item);
        }
        return result;
    }

    private void fillScores(PkLsRecord record, String gradeJson) {
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
            com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(json);
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

    private String generateRoomCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }
}
