package com.zhixue.ai.module.ai.service;

import com.zhixue.ai.module.ai.entity.AiListeningSpeaking;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 英语听说练习服务
 */
public interface ListeningSpeakingService {

    /** 题目列表(支持学段过滤) */
    List<AiListeningSpeaking> listQuestions(Integer gradeLevel);

    /** 题目详情 */
    AiListeningSpeaking getQuestionDetail(Long id);

    /** 提交音频作答(AI 识别 + 评分) */
    Map<String, Object> submitAnswer(Long userId, Long questionId, MultipartFile audioFile, String supplementText);

    /** 我的作答记录 */
    List<Map<String, Object>> listMyRecords(Long userId);

    /** 上传音频文件到 upload/audio/ 目录,返回相对路径 */
    String uploadAudio(MultipartFile file);

    // ===================== 学生自主出题 =====================

    /** AI 自定义文本出题 */
    Map<String, Object> generateFromText(Long userId, String text, String questionType, Integer gradeLevel);

    /** AI 按话题出题 */
    Map<String, Object> generateFromTopic(Long userId, String topic, String questionType, Integer difficulty, Integer gradeLevel);

    /** AI 图片出题 */
    Map<String, Object> generateFromImage(Long userId, String imageBase64, String questionType, Integer gradeLevel);

    /** AI 生成同类薄弱练习 */
    Map<String, Object> generateSimilar(Long userId, Long previousQuestionId);

    /** 获取话题列表 */
    List<String> getTopics(Integer gradeLevel);
}
