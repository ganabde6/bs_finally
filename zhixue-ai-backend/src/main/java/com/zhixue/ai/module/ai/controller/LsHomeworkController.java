package com.zhixue.ai.module.ai.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.entity.AiLsHomework;
import com.zhixue.ai.module.ai.entity.AiLsHomeworkQuestion;
import com.zhixue.ai.module.ai.service.LsHomeworkService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 教师端 - 英语听说作业 Controller
 */
@RestController
@RequestMapping("/api/teacher/listening-speaking-homework")
@RequiredArgsConstructor
public class LsHomeworkController {

    private final LsHomeworkService lsHomeworkService;

    /** 创建作业(草稿) */
    @PostMapping("/create")
    public Result<Long> create(@RequestBody AiLsHomework homework) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(lsHomeworkService.createHomework(teacherId, homework));
    }

    /** AI 生成题目 */
    @PostMapping("/generate")
    public Result<List<Map<String, Object>>> generate(@RequestBody Map<String, Object> params) {
        String mode = (String) params.get("mode");
        String groupParams = (String) params.get("params");
        return Result.success(lsHomeworkService.generateQuestions(mode, groupParams));
    }

    /** 保存题目到作业 */
    @PostMapping("/{homeworkId}/questions")
    public Result<Void> saveQuestions(@PathVariable Long homeworkId, @RequestBody List<AiLsHomeworkQuestion> questions) {
        lsHomeworkService.saveQuestions(homeworkId, questions);
        return Result.success(null);
    }

    /** 获取作业详情(含题目) */
    @GetMapping("/{homeworkId}")
    public Result<Map<String, Object>> detail(@PathVariable Long homeworkId) {
        return Result.success(lsHomeworkService.getHomeworkDetail(homeworkId));
    }

    /** 发布作业 */
    @PostMapping("/{homeworkId}/publish")
    public Result<Void> publish(@PathVariable Long homeworkId) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        lsHomeworkService.publishHomework(homeworkId, teacherId);
        return Result.success(null);
    }

    /** 教师作业列表 */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(lsHomeworkService.listTeacherHomework(teacherId));
    }

    /** 复制历史作业 */
    @PostMapping("/{homeworkId}/copy")
    public Result<Long> copy(@PathVariable Long homeworkId, @RequestParam(defaultValue = "false") boolean regenerate) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(lsHomeworkService.copyHomework(homeworkId, teacherId, regenerate));
    }

    /** 班级作业报告 */
    @GetMapping("/{homeworkId}/report")
    public Result<List<Map<String, Object>>> report(@PathVariable Long homeworkId) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(lsHomeworkService.getClassHomeworkReport(homeworkId, teacherId));
    }

    /** 学生提交听说作业作答 */
    @PostMapping("/submit/{questionId}")
    public Result<Map<String, Object>> submit(@PathVariable Long questionId,
                                              @RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "supplementText", required = false) String supplementText) {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(lsHomeworkService.submitHomeworkAnswer(studentId, questionId, file, supplementText));
    }

    /** 学生查看收到的听说作业 */
    @GetMapping("/student/list")
    public Result<List<Map<String, Object>>> studentList() {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(lsHomeworkService.listStudentHomework(studentId));
    }

    /** 学生查看某次作业的作答记录 */
    @GetMapping("/student/{homeworkId}/records")
    public Result<List<Map<String, Object>>> studentRecords(@PathVariable Long homeworkId) {
        Long studentId = SecurityUtils.getCurrentUserId();
        return Result.success(lsHomeworkService.listStudentHomeworkRecords(studentId, homeworkId));
    }
}
