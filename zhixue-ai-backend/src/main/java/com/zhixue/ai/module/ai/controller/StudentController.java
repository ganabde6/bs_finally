package com.zhixue.ai.module.ai.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.service.AiService;
import com.zhixue.ai.module.exam.service.ExamService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 Controller
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final ExamService examService;
    private final AiService aiService;

    /** 学习首页统计 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long studentId = SecurityUtils.getCurrentUserId();
        Long classId = SecurityUtils.getCurrentClassId();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("paperCount", examService.listStudentPapers(classId, studentId).size());
        result.put("answerCount", examService.listStudentAnswers(studentId).size());
        result.put("errorCount", aiService.listErrorBooks(studentId).size());
        return Result.success(result);
    }

    /** 查询本人班级已发布试卷(排除已提交的) */
    @GetMapping("/papers")
    public Result<?> papers() {
        Long studentId = SecurityUtils.getCurrentUserId();
        Long classId = SecurityUtils.getCurrentClassId();
        return Result.success(examService.listStudentPapers(classId, studentId));
    }

    /** 试卷详情(含题目,不含答案) */
    @GetMapping("/paper/{paperId}")
    public Result<Map<String, Object>> paperDetail(@PathVariable Long paperId) {
        return Result.success(examService.getPaperDetail(paperId));
    }

    /** 开始/继续作答 */
    @PostMapping("/answer/start/{paperId}")
    public Result<?> startAnswer(@PathVariable Long paperId) {
        return Result.success(examService.startAnswer(paperId, SecurityUtils.getCurrentUserId()));
    }

    /** 提交作答 */
    @PostMapping("/answer/{answerId}/submit")
    public Result<?> submitAnswer(@PathVariable Long answerId,
                                  @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        Map<Long, String> answers = (Map<Long, String>) body.get("answers");
        Integer duration = (Integer) body.get("duration");
        examService.submitAnswer(answerId, duration, answers);
        // 自动触发 AI 批改
        return Result.success(aiService.correctAnswer(answerId, answers));
    }

    /** 我的作答记录 */
    @GetMapping("/answers")
    public Result<?> myAnswers() {
        return Result.success(examService.listStudentAnswers(SecurityUtils.getCurrentUserId()));
    }

    /** 作答批改详情 */
    @GetMapping("/answer/{answerId}/correct")
    public Result<?> correctDetail(@PathVariable Long answerId) {
        return Result.success(aiService.getCorrectDetails(answerId));
    }

    /** 错题本 */
    @GetMapping("/errorbooks")
    public Result<?> errorBooks() {
        return Result.success(aiService.listErrorBooks(SecurityUtils.getCurrentUserId()));
    }

    /** 标记错题已复盘 */
    @PutMapping("/errorbook/{id}/review")
    public Result<Void> reviewError(@PathVariable Long id, @RequestParam Integer status) {
        aiService.reviewErrorBook(id, status);
        return Result.success();
    }

    /** 推送变式题 */
    @PostMapping("/errorbook/{id}/variant")
    public Result<?> pushVariant(@PathVariable Long id) {
        return Result.success(aiService.pushVariant(id));
    }

    /** 变式题作答批改 */
    @PostMapping("/variant/{id}/answer")
    public Result<?> answerVariant(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.success(aiService.submitVariantAnswer(id, SecurityUtils.getCurrentUserId(), body.get("answer")));
    }

    /** 我的变式题 */
    @GetMapping("/variants")
    public Result<?> myVariants() {
        return Result.success(aiService.listVariants(SecurityUtils.getCurrentUserId()));
    }

    /** 个人学情分析 */
    @GetMapping("/study/analysis")
    public Result<?> studyAnalysis(@RequestParam(required = false) Long subjectId) {
        return Result.success(aiService.getStudentAnalysis(SecurityUtils.getCurrentUserId(), subjectId));
    }

    /** AI 助学问答 */
    @PostMapping("/tutor/chat")
    public Result<?> tutorChat(@RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        Integer chatType = (Integer) body.get("chatType");
        return Result.success(aiService.tutorChat(SecurityUtils.getCurrentUserId(), question, chatType));
    }

    /** AI 作文润色 */
    @PostMapping("/tutor/polish")
    public Result<?> polish(@RequestBody Map<String, String> body) {
        return Result.success(aiService.polishEssay(body.get("content")));
    }

    /** AI 历史对话 */
    @GetMapping("/tutor/history")
    public Result<?> chatHistory() {
        return Result.success(aiService.listChatHistory(SecurityUtils.getCurrentUserId()));
    }

    /** 上报风控事件(切屏/离开等) */
    @PostMapping("/risk/report")
    public Result<?> reportRisk(@RequestBody Map<String, Object> body) {
        Long answerId = Long.valueOf(body.get("answerId").toString());
        Integer riskType = (Integer) body.get("riskType");
        String desc = (String) body.get("description");
        return Result.success(aiService.reportRisk(answerId, SecurityUtils.getCurrentUserId(), riskType, desc));
    }
}
