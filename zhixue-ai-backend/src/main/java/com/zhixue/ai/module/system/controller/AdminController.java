package com.zhixue.ai.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.entity.AiModelConfig;
import com.zhixue.ai.module.ai.service.AiService;
import com.zhixue.ai.module.monitor.service.DashboardService;
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
    private final AiService aiService;
    private final DashboardService dashboardService;

    // ============== 数据大屏 ==============

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.success(dashboardService.globalStats());
    }

    @GetMapping("/dashboard/paper-distribution")
    public Result<?> paperDistribution() {
        return Result.success(dashboardService.paperDistribution());
    }

    @GetMapping("/dashboard/class-ranking")
    public Result<?> classRanking() {
        return Result.success(dashboardService.classRanking());
    }

    @GetMapping("/dashboard/risk-distribution")
    public Result<?> riskDistribution() {
        return Result.success(dashboardService.riskTypeDistribution());
    }

    @GetMapping("/dashboard/recent-logs")
    public Result<?> recentLogs() {
        return Result.success(dashboardService.recentLogs(10));
    }

    // ============== 用户管理 ==============

    @GetMapping("/users")
    public Result<Page<SysUser>> pageUsers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) Long classId) {
        return Result.success(systemService.pageUsers(current, size, keyword, roleId, classId));
    }

    @GetMapping("/user/{id}")
    public Result<SysUser> getUser(@PathVariable Long id) {
        return Result.success(systemService.getUserById(id));
    }

    @PostMapping("/user")
    public Result<Void> addUser(@RequestBody SysUser user) {
        systemService.addUser(user);
        return Result.success();
    }

    @PutMapping("/user")
    public Result<Void> updateUser(@RequestBody SysUser user) {
        systemService.updateUser(user);
        return Result.success();
    }

    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        systemService.deleteUser(id);
        return Result.success();
    }

    @PutMapping("/user/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        systemService.resetPassword(id, body.get("password"));
        return Result.success();
    }

    /** 班级学生列表 */
    @GetMapping("/class/{classId}/students")
    public Result<?> classStudents(@PathVariable Long classId) {
        return Result.success(systemService.listClassStudents(classId));
    }

    /** 闲置账号清理(返回长期未登录用户列表) */
    @GetMapping("/users/idle")
    public Result<?> idleUsers() {
        return Result.success(systemService.pageUsers(1L, 100L, null, null, null));
    }

    // ============== 角色权限 ==============

    @GetMapping("/roles")
    public Result<List<SysRole>> roles() {
        return Result.success(systemService.listRoles());
    }

    @PostMapping("/role")
    public Result<Void> addRole(@RequestBody SysRole role) {
        systemService.addRole(role);
        return Result.success();
    }

    @PutMapping("/role")
    public Result<Void> updateRole(@RequestBody SysRole role) {
        systemService.updateRole(role);
        return Result.success();
    }

    @DeleteMapping("/role/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        systemService.deleteRole(id);
        return Result.success();
    }

    @GetMapping("/permissions")
    public Result<?> permissions() {
        return Result.success(systemService.treePermissions());
    }

    @GetMapping("/role/{roleId}/permissions")
    public Result<List<Long>> rolePermissions(@PathVariable Long roleId) {
        return Result.success(systemService.getRolePermissionIds(roleId));
    }

    @PutMapping("/role/{roleId}/permissions")
    public Result<Void> assignPermissions(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        systemService.assignRolePermissions(roleId, body.get("permissionIds"));
        return Result.success();
    }

    // ============== 班级管理 ==============

    @GetMapping("/classes")
    public Result<List<SysClass>> classes() {
        return Result.success(systemService.listClasses());
    }

    @PostMapping("/class")
    public Result<Void> addClass(@RequestBody SysClass cls) {
        systemService.addClass(cls);
        return Result.success();
    }

    @PutMapping("/class")
    public Result<Void> updateClass(@RequestBody SysClass cls) {
        systemService.updateClass(cls);
        return Result.success();
    }

    @DeleteMapping("/class/{id}")
    public Result<Void> deleteClass(@PathVariable Long id) {
        systemService.deleteClass(id);
        return Result.success();
    }

    // ============== 学科/课程管理 ==============

    @GetMapping("/subjects")
    public Result<List<SysSubject>> subjects() {
        return Result.success(systemService.listSubjects());
    }

    @PostMapping("/subject")
    public Result<Void> addSubject(@RequestBody SysSubject subject) {
        systemService.addSubject(subject);
        return Result.success();
    }

    @PutMapping("/subject")
    public Result<Void> updateSubject(@RequestBody SysSubject subject) {
        systemService.updateSubject(subject);
        return Result.success();
    }

    @DeleteMapping("/subject/{id}")
    public Result<Void> deleteSubject(@PathVariable Long id) {
        systemService.deleteSubject(id);
        return Result.success();
    }

    /** 教师任课列表 */
    @GetMapping("/teacher/{teacherId}/courses")
    public Result<?> teacherCourses(@PathVariable Long teacherId) {
        return Result.success(systemService.listTeacherCourses(teacherId));
    }

    @PostMapping("/teacher-course")
    public Result<Void> assignCourse(@RequestBody Map<String, Object> body) {
        systemService.assignTeacherCourse(
                Long.valueOf(body.get("teacherId").toString()),
                Long.valueOf(body.get("classId").toString()),
                Long.valueOf(body.get("subjectId").toString()));
        return Result.success();
    }

    @DeleteMapping("/teacher-course/{id}")
    public Result<Void> removeCourse(@PathVariable Long id) {
        systemService.removeTeacherCourse(id);
        return Result.success();
    }

    // ============== AI 配置 ==============

    @GetMapping("/ai-configs")
    public Result<List<AiModelConfig>> aiConfigs() {
        return Result.success(aiService.listAiConfigs());
    }

    @PutMapping("/ai-config")
    public Result<Void> updateAiConfig(@RequestBody AiModelConfig config) {
        aiService.updateAiConfig(config);
        return Result.success();
    }

    // ============== 公告管理 ==============

    @GetMapping("/notices")
    public Result<Page<SysNotice>> pageNotices(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        return Result.success(systemService.pageNotices(current, size));
    }

    @PostMapping("/notice")
    public Result<Void> addNotice(@RequestBody SysNotice notice) {
        systemService.addNotice(notice);
        return Result.success();
    }

    @PutMapping("/notice")
    public Result<Void> updateNotice(@RequestBody SysNotice notice) {
        systemService.updateNotice(notice);
        return Result.success();
    }

    @DeleteMapping("/notice/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        systemService.deleteNotice(id);
        return Result.success();
    }

    // ============== 内容风控 ==============

    /** 测试内容风控(管理员预检) */
    @PostMapping("/moderation/check")
    public Result<?> moderationCheck(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        com.zhixue.ai.module.ai.engine.AiServiceProvider provider =
                new com.zhixue.ai.module.ai.engine.LocalRuleAiServiceProvider();
        String risk = provider.moderateContent(content);
        return Result.success(java.util.Map.of("passed", risk == null, "reason", risk == null ? "" : risk));
    }

    // ============== 系统日志 ==============

    @GetMapping("/logs")
    public Result<Page<SysLog>> pageLogs(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String module) {
        return Result.success(systemService.pageLogs(current, size, module));
    }
}
