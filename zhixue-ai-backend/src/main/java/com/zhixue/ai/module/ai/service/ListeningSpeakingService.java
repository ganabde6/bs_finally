package com.zhixue.ai.module.ai.service;

import com.zhixue.ai.module.ai.entity.AiListeningSpeaking;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 高考英语听说练习服务
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
}
