package com.zhixue.ai.module.ai.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.entity.AiListeningSpeaking;
import com.zhixue.ai.module.ai.service.ListeningSpeakingService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 学生端 - 英语听说练习 Controller
 */
@RestController
@RequestMapping("/api/student/listening-speaking")
@RequiredArgsConstructor
public class ListeningSpeakingController {

    private final ListeningSpeakingService listeningSpeakingService;

    /** 题目列表(支持学段过滤) */
    @GetMapping("/list")
    public Result<List<AiListeningSpeaking>> list(@RequestParam(required = false) Integer gradeLevel) {
        return Result.success(listeningSpeakingService.listQuestions(gradeLevel));
    }

    /** 题目详情 */
    @GetMapping("/{id}")
    public Result<AiListeningSpeaking> detail(@PathVariable Long id) {
        return Result.success(listeningSpeakingService.getQuestionDetail(id));
    }

    /** 提交音频作答(音频文件 + 文字补充) */
    @PostMapping("/{id}/submit")
    public Result<Map<String, Object>> submit(@PathVariable Long id,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "supplementText", required = false) String supplementText) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(listeningSpeakingService.submitAnswer(userId, id, file, supplementText));
    }

    /** 我的作答记录 */
    @GetMapping("/records")
    public Result<List<Map<String, Object>>> records() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(listeningSpeakingService.listMyRecords(userId));
    }

    // ===================== 学生自主出题 =====================

    /** AI 自定义文本出题 */
    @PostMapping("/generate/text")
    public Result<Map<String, Object>> generateFromText(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtils.getCurrentUserId();
        String text = (String) params.get("text");
        String questionType = (String) params.get("questionType");
        Integer gradeLevel = params.get("gradeLevel") != null ? Integer.valueOf(params.get("gradeLevel").toString()) : null;
        return Result.success(listeningSpeakingService.generateFromText(userId, text, questionType, gradeLevel));
    }

    /** AI 按话题出题 */
    @PostMapping("/generate/topic")
    public Result<Map<String, Object>> generateFromTopic(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtils.getCurrentUserId();
        String topic = (String) params.get("topic");
        String questionType = (String) params.get("questionType");
        Integer difficulty = params.get("difficulty") != null ? Integer.valueOf(params.get("difficulty").toString()) : null;
        Integer gradeLevel = params.get("gradeLevel") != null ? Integer.valueOf(params.get("gradeLevel").toString()) : null;
        return Result.success(listeningSpeakingService.generateFromTopic(userId, topic, questionType, difficulty, gradeLevel));
    }

    /** AI 图片出题 */
    @PostMapping("/generate/image")
    public Result<Map<String, Object>> generateFromImage(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtils.getCurrentUserId();
        String imageBase64 = (String) params.get("imageBase64");
        String questionType = (String) params.get("questionType");
        Integer gradeLevel = params.get("gradeLevel") != null ? Integer.valueOf(params.get("gradeLevel").toString()) : null;
        return Result.success(listeningSpeakingService.generateFromImage(userId, imageBase64, questionType, gradeLevel));
    }

    /** AI 生成同类薄弱练习 */
    @PostMapping("/generate/similar")
    public Result<Map<String, Object>> generateSimilar(@RequestBody Map<String, Object> params) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long previousQuestionId = Long.valueOf(params.get("previousQuestionId").toString());
        return Result.success(listeningSpeakingService.generateSimilar(userId, previousQuestionId));
    }

    /** 获取话题列表 */
    @GetMapping("/topics")
    public Result<List<String>> topics(@RequestParam(required = false) Integer gradeLevel) {
        return Result.success(listeningSpeakingService.getTopics(gradeLevel));
    }
}
