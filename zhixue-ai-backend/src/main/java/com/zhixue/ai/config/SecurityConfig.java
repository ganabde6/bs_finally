package com.zhixue.ai.config;

import com.alibaba.fastjson2.JSON;
import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.common.result.ResultCode;
import com.zhixue.ai.security.JwtAuthenticationFilter;
import com.zhixue.ai.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .cors().and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
            .authorizeRequests()
                // 放行:登录、注册、静态资源、swagger、上传文件访问
                .antMatchers(
                        "/api/auth/**",
                        "/api/common/subjects",
                        "/api/common/classes",
                        "/upload/**",
                        "/ws/**",
                        "/favicon.ico",
                        "/error"
                ).permitAll()
                // 三端接口分别由角色控制
                .antMatchers("/api/admin/**").hasAnyRole("SUPER_ADMIN", "SCHOOL_ADMIN")
                .antMatchers("/api/teacher/**").hasRole("TEACHER")
                .antMatchers("/api/student/**").hasRole("STUDENT")
                .antMatchers("/api/common/**").authenticated()
                .anyRequest().authenticated()
            .and()
            .exceptionHandling()
                .authenticationEntryPoint((req, resp, e) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(200);
                    resp.getWriter().write(JSON.toJSONString(Result.error(ResultCode.UNAUTHORIZED)));
                })
                .accessDeniedHandler((req, resp, e) -> {
                    resp.setContentType("application/json;charset=UTF-8");
                    resp.setStatus(200);
                    resp.getWriter().write(JSON.toJSONString(Result.error(ResultCode.FORBIDDEN)));
                });
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
