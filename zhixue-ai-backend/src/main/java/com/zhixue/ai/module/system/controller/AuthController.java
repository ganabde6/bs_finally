package com.zhixue.ai.module.system.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.service.AuthService;
import com.zhixue.ai.module.system.service.SystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证 Controller(无需登录)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SystemService systemService;

    /** 登录 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        return Result.success(authService.login(body.get("username"), body.get("password")));
    }

    /** 学生注册(仅允许注册学生身份) */
    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> body) {
        authService.registerStudent(
                body.get("username"),
                body.get("password"),
                body.get("realName"),
                body.get("classId") != null ? Long.valueOf(body.get("classId")) : null);
        return Result.success();
    }

    /** 获取当前用户信息 */
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.success(authService.getCurrentUserInfo());
    }

    /** 修改个人资料 */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody SysUser user) {
        systemService.updateProfile(user);
        return Result.success();
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        systemService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return Result.success();
    }

    /** 登出(前端清除 token 即可) */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
