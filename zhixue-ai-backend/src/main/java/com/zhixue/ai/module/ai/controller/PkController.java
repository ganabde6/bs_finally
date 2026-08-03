package com.zhixue.ai.module.ai.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.service.PkService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 - 同学PK Controller
 */
@RestController
@RequestMapping("/api/student/pk")
@RequiredArgsConstructor
public class PkController {

    private final PkService pkService;

    /** 创建PK房间 */
    @PostMapping("/create")
    public Result<Map<String, Object>> createRoom(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long subjectId = Long.valueOf(body.get("subjectId").toString());
        Integer questionCount = body.get("questionCount") != null
                ? Integer.valueOf(body.get("questionCount").toString()) : 10;
        Integer timeLimitSeconds = body.get("timeLimitSeconds") != null
                ? Integer.valueOf(body.get("timeLimitSeconds").toString()) : 600;
        return Result.success(pkService.createRoom(userId, subjectId, questionCount, timeLimitSeconds));
    }

    /** 加入PK房间 */
    @PostMapping("/join")
    public Result<Map<String, Object>> joinRoom(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(pkService.joinRoom(userId, body.get("roomCode")));
    }

    /** 获取房间题目 */
    @GetMapping("/questions")
    public Result<List<Map<String, Object>>> getRoomQuestions(@RequestParam String roomCode) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(pkService.getRoomQuestions(userId, roomCode));
    }

    /** 提交PK答案 */
    @PostMapping("/answer")
    public Result<Map<String, Object>> submitAnswer(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String roomCode = body.get("roomCode").toString();
        Long questionId = Long.valueOf(body.get("questionId").toString());
        String userAnswer = body.get("userAnswer") != null ? body.get("userAnswer").toString() : "";
        return Result.success(pkService.submitAnswer(userId, roomCode, questionId, userAnswer));
    }

    /** 查询房间排名 */
    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> getRanking(@RequestParam String roomCode) {
        return Result.success(pkService.getRoomRanking(roomCode));
    }

    /** 获取房间状态 */
    @GetMapping("/status")
    public Result<Map<String, Object>> getRoomStatus(@RequestParam String roomCode) {
        return Result.success(pkService.getRoomStatus(roomCode));
    }
}
