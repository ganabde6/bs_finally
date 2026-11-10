package com.zhixue.ai.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.log.OperationLog;
import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.entity.ContentRiskLog;
import com.zhixue.ai.module.ai.service.ContentRiskService;
import com.zhixue.ai.module.monitor.service.DashboardService;
import com.zhixue.ai.module.monitor.service.HomeworkMonitorService;
import com.zhixue.ai.module.system.entity.*;
import com.zhixue.ai.module.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 后台管理端 Controller
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final SystemService systemService;
    private final ContentRiskService contentRiskService;
    private final HomeworkMonitorService homeworkMonitorService;
    private final DashboardService dashboardService;

    // ============== 数据大屏 ==============

    @OperationLog(module = "数据大屏", operation = "加载数据大屏")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(dashboardService.globalStats());
    }

    @OperationLog(module = "数据大屏", operation = "加载试卷分布统计")
    @GetMapping("/dashboard/paper-distribution")
    public Result<?> paperDistribution() {
        return Result.success(dashboardService.paperDistribution());
    }

    @OperationLog(module = "数据大屏", operation = "加载班级排名统计")
    @GetMapping("/dashboard/class-ranking")
    public Result<?> classRanking() {
        return Result.success(dashboardService.classRanking());
    }

    @OperationLog(module = "数据大屏", operation = "加载风控类型分布")
    @GetMapping("/dashboard/risk-distribution")
    public Result<?> riskDistribution() {
        return Result.success(dashboardService.riskTypeDistribution());
    }

    @OperationLog(module = "数据大屏", operation = "加载最近操作日志")
    @GetMapping("/dashboard/recent-logs")
    public Result<?> recentLogs() {
        return Result.success(dashboardService.recentLogs(10));
    }

    // ============== 用户管理 ==============

    @OperationLog(module = "用户管理", operation = "查询用户列表")
    @GetMapping("/users")
    public Result<Page<SysUser>> pageUsers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long classId) {
        return Result.success(systemService.pageUsers(current, size, keyword, roleId, classId));
    }

    @OperationLog(module = "用户管理", operation = "查看用户详情")
    @GetMapping("/user/{id}")
    public Result<SysUser> getUser(@PathVariable Long id) {
        return Result.success(systemService.getUserById(id));
    }

    @OperationLog(module = "用户管理", operation = "新增用户")
    @PostMapping("/user")
    public Result<Void> addUser(@RequestBody SysUser user) {
        systemService.addUser(user);
        return Result.success();
    }

    @OperationLog(module = "用户管理", operation = "编辑用户")
    @PutMapping("/user")
    public Result<Void> updateUser(@RequestBody SysUser user) {
        systemService.updateUser(user);
        return Result.success();
    }

    @OperationLog(module = "用户管理", operation = "删除用户")
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id);
        return Result.success();
    }

    @OperationLog(module = "用户管理", operation = "重置用户密码")
    @PutMapping("/user/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        systemService.resetPassword(id, body.get("password"));
        return Result.success();
    }

    /** 班级学生列表 */
    @OperationLog(module = "班级管理", operation = "查询班级学生列表")
    @GetMapping("/class/{classId}/students")
    public Result<?> classStudents(@PathVariable Long classId) {
        return Result.success(systemService.listClassStudents(classId));
    }

    /** 闲置账号清理(返回长期未登录用户列表) */
    @OperationLog(module = "用户管理", operation = "查询闲置账号")
    @GetMapping("/users/idle")
    public Result<?> idleUsers() {
        return Result.success(systemService.pageUsers(1L, 100L, null, null, null));
    }

    // ============== 班级管理 ==============

    @OperationLog(module = "班级管理", operation = "查询班级列表")
    @GetMapping("/classes")
    public Result<List<SysClass>> classes() {
        return Result.success(systemService.listClasses());
    }

    @OperationLog(module = "班级管理", operation = "新增班级")
    @PostMapping("/class")
    public Result<Void> addClass(@RequestBody SysClass cls) {
        systemService.addClass(cls);
        return Result.success();
    }

    @OperationLog(module = "班级管理", operation = "编辑班级")
    @PutMapping("/class")
    public Result<Void> updateClass(@RequestBody SysClass cls) {
        systemService.updateClass(cls);
        return Result.success();
    }

    @OperationLog(module = "班级管理", operation = "删除班级")
    @DeleteMapping("/class/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        systemService.deleteClass(id);
        return Result.success();
    }

    // ============== 学科/课程管理 ==============

    @OperationLog(module = "学科管理", operation = "查询学科列表")
    @GetMapping("/subjects")
    public Result<List<SysSubject>> subjects() {
        return Result.success(systemService.listSubjects());
    }

    @OperationLog(module = "学科管理", operation = "新增学科")
    @PostMapping("/subject")
    public Result<Void> addSubject(@RequestBody SysSubject subject) {
        systemService.addSubject(subject);
        return Result.success();
    }

    @OperationLog(module = "学科管理", operation = "编辑学科")
    @PutMapping("/subject")
    public Result<Void> updateSubject(@RequestBody SysSubject subject) {
        systemService.updateSubject(subject);
        return Result.success();
    }

    @OperationLog(module = "学科管理", operation = "删除学科")
    @DeleteMapping("/subject/{id}")
    public Result<Void> deleteSubject(@PathVariable Long id) {
        systemService.deleteSubject(id);
        return Result.success();
    }

    /** 教师任课列表 */
    @OperationLog(module = "任课管理", operation = "查询教师任课")
    @GetMapping("/teacher/{teacherId}/courses")
    public Result<?> teacherCourses(@PathVariable Long teacherId) {
        return Result.success(systemService.listTeacherCourses(teacherId));
    }

    @OperationLog(module = "任课管理", operation = "分配教师任课")
    @PostMapping("/teacher-course")
    public Result<Void> assignCourse(@RequestBody Map<String, Object> body) {
        systemService.assignTeacherCourse(
                Long.valueOf(body.get("teacherId").toString()),
                Long.valueOf(body.get("classId").toString()),
                Long.valueOf(body.get("subjectId").toString()));
        return Result.success();
    }

    @OperationLog(module = "任课管理", operation = "编辑教师任课")
    @PutMapping("/teacher-course/{id}")
    public Result<Void> updateCourse(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        systemService.updateTeacherCourse(
                id,
                Long.valueOf(body.get("classId").toString()),
                Long.valueOf(body.get("subjectId").toString()));
        return Result.success();
    }

    @OperationLog(module = "任课管理", operation = "移除教师任课")
    @DeleteMapping("/teacher-course/{id}")
    public Result<Void> removeCourse(@PathVariable Long id) {
        systemService.removeTeacherCourse(id);
        return Result.success();
    }

    // ============== 作业监测 ==============

    @OperationLog(module = "作业监测", operation = "查询作业监测概览")
    @GetMapping("/homework/summary")
    public Result<Map<String, Object>> homeworkSummary() {
        return Result.success(homeworkMonitorService.summary());
    }

    @OperationLog(module = "作业监测", operation = "查询作业任务列表")
    @GetMapping("/homework/tasks")
    public Result<Page<Map<String, Object>>> homeworkTasks(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String keyword) {
        return Result.success(homeworkMonitorService.pageTasks(current, size, classId, subjectId, keyword));
    }

    @OperationLog(module = "作业监测", operation = "查询班级完成率排行")
    @GetMapping("/homework/class-ranking")
    public Result<List<Map<String, Object>>> homeworkClassRanking() {
        return Result.success(homeworkMonitorService.classRanking());
    }

    // ============== 公告管理 ==============

    @OperationLog(module = "公告管理", operation = "查询公告列表")
    @GetMapping("/notices")
    public Result<Page<SysNotice>> pageNotices(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(systemService.pageNotices(current, size));
    }

    @OperationLog(module = "公告管理", operation = "发布公告")
    @PostMapping("/notice")
    public Result<Void> addNotice(@RequestBody SysNotice notice) {
        contentRiskService.audit(noticeText(notice), "发布公告");
        systemService.addNotice(notice);
        return Result.success();
    }

    @OperationLog(module = "公告管理", operation = "编辑公告")
    @PutMapping("/notice")
    public Result<Void> updateNotice(@RequestBody SysNotice notice) {
        contentRiskService.audit(noticeText(notice), "编辑公告");
        systemService.updateNotice(notice);
        return Result.success();
    }

    @OperationLog(module = "公告管理", operation = "删除公告")
    @DeleteMapping("/notice/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        systemService.deleteNotice(id);
        return Result.success();
    }

    // ============== 任务派发 ==============

    @OperationLog(module = "任务派发", operation = "查询任务列表")
    @GetMapping("/tasks")
    public Result<?> listTasks() {
        return Result.success(systemService.listTasks());
    }

    @OperationLog(module = "任务派发", operation = "派发任务")
    @PostMapping("/task")
    public Result<Void> addTask(@RequestBody SysTask task) {
        contentRiskService.audit(taskText(task), "派发任务");
        systemService.addTask(task);
        return Result.success();
    }

    @OperationLog(module = "任务派发", operation = "编辑任务")
    @PutMapping("/task")
    public Result<Void> updateTask(@RequestBody SysTask task) {
        contentRiskService.audit(taskText(task), "编辑任务");
        systemService.updateTask(task);
        return Result.success();
    }

    @OperationLog(module = "任务派发", operation = "删除任务")
    @DeleteMapping("/task/{id}")
    public Result<Void> deleteTask(@PathVariable Long id) {
        systemService.deleteTask(id);
        return Result.success();
    }

    // ============== 系统日志 ==============

    @OperationLog(module = "系统日志", operation = "查询操作日志")
    @GetMapping("/logs")
    public Result<Page<SysLog>> pageLogs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String module) {
        return Result.success(systemService.pageLogs(current, size, module));
    }

    @OperationLog(module = "风控管理", operation = "查询内容风控记录")
    @GetMapping("/content-risks")
    public Result<Page<ContentRiskLog>> pageContentRisks(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(contentRiskService.pageContentRisks(current, size));
    }

    /** 拼接公告标题与内容,供内容风控检测 */
    private String noticeText(SysNotice notice) {
        return (notice.getTitle() == null ? "" : notice.getTitle()) + " "
                + (notice.getContent() == null ? "" : notice.getContent());
    }

    /** 拼接任务标题与内容,供内容风控检测 */
    private String taskText(SysTask task) {
        return (task.getTitle() == null ? "" : task.getTitle()) + " "
                + (task.getContent() == null ? "" : task.getContent());
    }
}
