package com.zhixue.ai.security;

import com.zhixue.ai.common.utils.JwtUtils;
import com.zhixue.ai.module.system.entity.SysRole;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysRoleMapper;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.common.result.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security 用户加载服务
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final JwtUtils jwtUtils;

    /**
     * 根据用户名加载用户(登录时调用)
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ResultCode.ACCOUNT_DISABLED);
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new BizException("用户角色不存在");
        }
        return LoginUser.from(user, role.getRoleCode());
    }

    /**
     * 根据 token 加载用户(请求拦截时调用)
     */
    public LoginUser loadFromToken(String token) {
        if (jwtUtils.isExpired(token)) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        Long userId = jwtUtils.getUserId(token);
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        if (role == null) {
            throw new BizException("用户角色不存在");
        }
        return LoginUser.from(user, role.getRoleCode());
    }
}
