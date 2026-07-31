package com.zhixue.ai.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具:获取当前登录用户
 */
public class SecurityUtils {

    public static LoginUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) auth.getPrincipal();
    }

    public static Long getCurrentUserId() {
        LoginUser u = getCurrentUser();
        return u == null ? null : u.getUserId();
    }

    public static String getCurrentRoleCode() {
        LoginUser u = getCurrentUser();
        return u == null ? null : u.getRoleCode();
    }

    public static Long getCurrentRoleId() {
        LoginUser u = getCurrentUser();
        return u == null ? null : u.getRoleId();
    }

    public static Long getCurrentClassId() {
        LoginUser u = getCurrentUser();
        return u == null ? null : u.getClassId();
    }
}
