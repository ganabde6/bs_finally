package com.zhixue.ai.module.ai.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.service.PkLsService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 学生端 - 英语听说 PK Controller
 */
@RestController
@RequestMapping("/api/student/pk-ls")
@RequiredArgsConstructor
public class PkLsController {

    private final PkLsService pkLsService;

    /** 创建听说 PK 挑战 */
    @PostMapping("/create")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        String questionTitle = (String) body.get("questionTitle");
        String questionContent = (String) body.get("questionContent");
        String referenceText = (String) body.get("referenceText");
        String questionType = (String) body.get("questionType");
        String scorePoints = (String) body.get("scorePoints");
        return Result.success(pkLsService.createChallenge(userId, questionTitle, questionContent,
                referenceText, questionType, scorePoints));
    }

    /** 接受挑战 */
    @PostMapping("/accept")
    public Result<Map<String, Object>> accept(@RequestBody Map<String, String> body) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(pkLsService.acceptChallenge(userId, body.get("roomCode")));
    }

    /** 获取挑战详情 */
    @GetMapping("/detail")
    public Result<Map<String, Object>> detail(@RequestParam String roomCode) {
        return Result.success(pkLsService.getChallengeDetail(roomCode));
    }

    /** 提交听说 PK 作答 */
    @PostMapping("/submit")
    public Result<Map<String, Object>> submit(@RequestParam String roomCode,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "supplementText", required = false) String supplementText) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(pkLsService.submitPkAnswer(userId, roomCode, file, supplementText));
    }

    /** 获取 PK 结果 */
    @GetMapping("/result")
    public Result<Map<String, Object>> result(@RequestParam String roomCode) {
        return Result.success(pkLsService.getPkResult(roomCode));
    }

    /** 我的挑战列表 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(pkLsService.listMyChallenges(userId));
    }
}
