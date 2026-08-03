package com.zhixue.ai.module.ai.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.service.SelfPracticeService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 - 自主智练与自律打卡 Controller
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class SelfPracticeController {

    private final SelfPracticeService selfPracticeService;

    /** 智能生成练习题目（旧接口，保留兼容） */
    @PostMapping("/practice/generate")
    public Result<List<Map<String, Object>>> generatePractice() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(selfPracticeService.generatePractice(userId));
    }

    /** AI 智能组卷（新接口，支持参数化配置） */
    @PostMapping("/practice/generate-config")
    public Result<Map<String, Object>> generatePracticeConfig(@RequestBody Map<String, Object> config) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(selfPracticeService.generatePracticeByConfig(userId, config));
    }

    /** 获取知识点列表 */
    @GetMapping("/practice/knowledge-points")
    public Result<List<String>> getKnowledgePoints(@RequestParam(required = false) String subjectId) {
        if (subjectId == null || "null".equals(subjectId)) {
            return Result.success(java.util.Collections.emptyList());
        }
        return Result.success(selfPracticeService.getKnowledgePointsBySubject(Long.valueOf(subjectId)));
    }

    /** 获取最近练习记录 */
    @GetMapping("/practice/recent-records")
    public Result<List<Map<String, Object>>> getRecentPracticeRecords() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(selfPracticeService.getRecentPracticeRecords(userId));
    }

    /** 提交批改 */
    @PostMapping("/practice/submit")
    public Result<List<Map<String, Object>>> submitPractice(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questionAnswers = (List<Map<String, Object>>) body.get("questionAnswers");
        Integer durationSeconds = body.get("durationSeconds") != null
                ? Integer.valueOf(body.get("durationSeconds").toString()) : 0;
        return Result.success(selfPracticeService.submitPractice(userId, questionAnswers, durationSeconds));
    }

    /** 查询打卡状态 */
    @GetMapping("/checkin/status")
    public Result<Map<String, Object>> checkInStatus() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(selfPracticeService.getCheckInStatus(userId));
    }

    /** 自律打卡 */
    @PostMapping("/checkin/do")
    public Result<Map<String, Object>> doCheckIn() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(selfPracticeService.doCheckIn(userId));
    }
}
