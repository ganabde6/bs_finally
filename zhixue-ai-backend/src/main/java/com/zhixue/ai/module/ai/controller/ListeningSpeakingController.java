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
 * 学生端 - 高考英语听说练习 Controller
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
}
