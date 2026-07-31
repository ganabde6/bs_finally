package com.zhixue.ai.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zhixue.ai.module.system.entity.SysUser;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 登录用户(实现 UserDetails)
 */
@Data
public class LoginUser implements UserDetails {

    private Long userId;
    private String username;
    @JsonIgnore
    private String password;
    private Long roleId;
    private String roleCode;
    private String realName;
    private Integer status;
    private Long classId;
    private String avatar;

    public static LoginUser from(SysUser u, String roleCode) {
        LoginUser lu = new LoginUser();
        lu.setUserId(u.getId());
        lu.setUsername(u.getUsername());
        lu.setPassword(u.getPassword());
        lu.setRoleId(u.getRoleId());
        lu.setRoleCode(roleCode);
        lu.setRealName(u.getRealName());
        lu.setStatus(u.getStatus());
        lu.setClassId(u.getClassId());
        lu.setAvatar(u.getAvatar());
        return lu;
    }

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() { return true; }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() { return true; }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @JsonIgnore
    @Override
    public boolean isEnabled() { return status != null && status == 1; }
}
