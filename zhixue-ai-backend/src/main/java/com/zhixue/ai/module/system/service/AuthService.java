package com.zhixue.ai.module.system.service;

import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.common.result.ResultCode;
import com.zhixue.ai.common.utils.JwtUtils;
import com.zhixue.ai.module.system.entity.SysPermission;
import com.zhixue.ai.module.system.entity.SysRole;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysPermissionMapper;
import com.zhixue.ai.module.system.mapper.SysRoleMapper;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import com.zhixue.ai.security.LoginUser;
import com.zhixue.ai.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * 登录
     */
    public Map<String, Object> login(String username, String password) {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new BizException("用户角色不存在");
        }
        // 更新最后登录时间
        SysUser upd = new SysUser();
        upd.setId(user.getId());
        upd.setLastLogin(LocalDateTime.now());
        userMapper.updateById(upd);
        // 生成 token
        String token = jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRoleId());
        // 查询权限
        List<SysPermission> perms = permissionMapper.selectByRoleId(user.getRoleId());
        // 组装返回
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("roleId", user.getRoleId());
        userInfo.put("roleCode", role.getRoleCode());
        userInfo.put("roleName", role.getRoleName());
        userInfo.put("classId", user.getClassId());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("permissions", perms);
        result.put("userInfo", userInfo);
        return result;
    }

    /**
     * 学生注册(仅允许注册学生身份,roleId 固定为 4)
     */
    public void registerStudent(String username, String password, String realName, Long classId) {
        if (username == null || username.trim().isEmpty()) {
            throw new BizException("用户名不能为空");
        }
        if (password == null || password.length() < 6) {
            throw new BizException("密码长度不能少于6位");
        }
        if (userMapper.selectByUsername(username) != null) {
            throw new BizException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setPassword(passwordEncoder.encode(password));
        user.setRealName(realName);
        user.setRoleId(4L); // 固定为学生角色
        user.setClassId(classId);
        user.setStatus(1);
        userMapper.insert(user);
    }

    /**
     * 获取当前登录用户信息(从 SecurityContext 读取)
     */
    public Map<String, Object> getCurrentUserInfo() {
        com.zhixue.ai.security.LoginUser lu = com.zhixue.ai.security.SecurityUtils.getCurrentUser();
        if (lu == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("userId", lu.getUserId());
        result.put("username", lu.getUsername());
        result.put("realName", lu.getRealName());
        result.put("roleId", lu.getRoleId());
        result.put("roleCode", lu.getRoleCode());
        result.put("classId", lu.getClassId());
        result.put("avatar", lu.getAvatar());
        // 查询权限
        List<SysPermission> perms = permissionMapper.selectByRoleId(lu.getRoleId());
        result.put("permissions", perms);
        return result;
    }
}
