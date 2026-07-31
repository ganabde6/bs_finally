package com.zhixue.ai.module.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.service.AiService;
import com.zhixue.ai.module.exam.entity.ExamPaper;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import com.zhixue.ai.module.exam.service.ExamService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端 Controller
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final ExamService examService;
    private final AiService aiService;

    /** 教师首页统计 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        Map<String, Object> result = new java.util.HashMap<>();
        Page<com.zhixue.ai.module.exam.entity.ExamPaper> page =
                examService.pagePapers(1L, 1000L, teacherId, null, null);
        result.put("paperCount", page.getTotal());
        return Result.success(result);
    }

    // ============== 题库管理 ==============

    @GetMapping("/questions")
    public Result<Page<ExamQuestion>> pageQuestions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) String keyword) {
        return Result.success(examService.pageQuestions(current, size, subjectId, questionType, keyword));
    }

    @GetMapping("/question/{id}")
    public Result<ExamQuestion> getQuestion(@PathVariable Long id) {
        return Result.success(examService.getQuestion(id));
    }

    @PostMapping("/question")
    public Result<Void> addQuestion(@RequestBody ExamQuestion q) {
        examService.addQuestion(q);
        return Result.success();
    }

    @PutMapping("/question")
    public Result<Void> updateQuestion(@RequestBody ExamQuestion q) {
        examService.updateQuestion(q);
        return Result.success();
    }

    @DeleteMapping("/question/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        examService.deleteQuestion(id);
        return Result.success();
    }

    /** AI 智能组卷(随机抽题) */
    @PostMapping("/questions/ai-group")
    public Result<List<Long>> aiGroup(@RequestBody Map<String, Object> body) {
        Long subjectId = Long.valueOf(body.get("subjectId").toString());
        Integer total = (Integer) body.get("totalQuestions");
        Integer difficulty = (Integer) body.get("difficulty");
        return Result.success(examService.aiGroupPaper(subjectId, total, difficulty));
    }

    // ============== 试卷管理 ==============

    @GetMapping("/papers")
    public Result<Page<ExamPaper>> pagePapers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer paperType,
            @RequestParam(required = false) Long classId) {
        return Result.success(examService.pagePapers(current, size, SecurityUtils.getCurrentUserId(), classId, paperType));
    }

    @GetMapping("/paper/{id}")
    public Result<Map<String, Object>> paperDetail(@PathVariable Long id) {
        return Result.success(examService.getPaperDetailForTeacher(id));
    }

    @PostMapping("/paper")
    public Result<Long> createPaper(@RequestBody Map<String, Object> body) {
        com.alibaba.fastjson2.JSON.parseObject(com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")));
        ExamPaper paper = com.alibaba.fastjson2.JSON.parseObject(
                com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")), ExamPaper.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        return Result.success(examService.createPaper(paper, questions));
    }

    @PutMapping("/paper")
    public Result<Void> updatePaper(@RequestBody Map<String, Object> body) {
        ExamPaper paper = com.alibaba.fastjson2.JSON.parseObject(
                com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")), ExamPaper.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        examService.updatePaper(paper, questions);
        return Result.success();
    }

    @DeleteMapping("/paper/{id}")
    public Result<Void> deletePaper(@PathVariable Long id) {
        examService.deletePaper(id);
        return Result.success();
    }

    @PutMapping("/paper/{id}/publish")
    public Result<Void> publishPaper(@PathVariable Long id) {
        examService.publishPaper(id);
        return Result.success();
    }

    @PutMapping("/paper/{id}/finish")
    public Result<Void> finishPaper(@PathVariable Long id) {
        examService.finishPaper(id);
        return Result.success();
    }

    // ============== 批改管理 ==============

    /** 试卷作答列表 */
    @GetMapping("/correct/{paperId}/answers")
    public Result<?> paperAnswers(@PathVariable Long paperId) {
        return Result.success(examService.listPaperAnswers(paperId));
    }

    /** 批改详情 */
    @GetMapping("/correct/{answerId}")
    public Result<?> correctDetail(@PathVariable Long answerId) {
        return Result.success(aiService.getCorrectDetails(answerId));
    }

    /** 批量 AI 批改 */
    @PostMapping("/correct/{paperId}/batch")
    public Result<?> batchCorrect(@PathVariable Long paperId) {
        return Result.success(aiService.batchCorrectPaper(paperId));
    }

    /** 手动微调批改 */
    @PutMapping("/correct/{correctId}/adjust")
    public Result<Void> adjustCorrect(@PathVariable Long correctId,
                                       @RequestBody Map<String, Object> body) {
        java.math.BigDecimal score = new java.math.BigDecimal(body.get("score").toString());
        String remark = (String) body.get("remark");
        aiService.adjustCorrect(correctId, score, remark);
        return Result.success();
    }

    /** 触发答案雷同查重 */
    @PostMapping("/correct/{paperId}/similarity")
    public Result<?> similarityCheck(@PathVariable Long paperId) {
        int total = 0;
        for (var a : examService.listPaperAnswers(paperId)) {
            total += aiService.similarityCheck(a.getId());
        }
        return Result.success(java.util.Map.of("similarCount", total));
    }

    // ============== 班级学情 ==============

    @GetMapping("/class/{classId}/analysis")
    public Result<?> classAnalysis(@PathVariable Long classId,
                                   @RequestParam(required = false) Long subjectId) {
        return Result.success(aiService.getClassAnalysis(classId, subjectId));
    }

    // ============== 家校反馈 ==============

    @GetMapping("/feedback/{studentId}")
    public Result<String> feedback(@PathVariable Long studentId) {
        return Result.success(aiService.generateComment(studentId));
    }
}
