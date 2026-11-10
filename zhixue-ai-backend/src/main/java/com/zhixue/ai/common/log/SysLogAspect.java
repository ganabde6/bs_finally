package com.zhixue.ai.common.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixue.ai.module.system.entity.SysLog;
import com.zhixue.ai.module.system.service.SystemService;
import com.zhixue.ai.security.LoginUser;
import com.zhixue.ai.security.SecurityUtils;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面:拦截标注了 @OperationLog 的方法,
 * 自动记录操作人(userId/username)、模块、操作、方法、参数、IP、耗时到 sys_log
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SysLogAspect {

    private final SystemService systemService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint pjp, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            try {
                record(pjp, operationLog, System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 日志记录失败不影响业务
                log.warn("操作日志记录失败: {}", e.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint pjp, OperationLog operationLog, long costMs) {
        SysLog entity = new SysLog();
        LoginUser user = SecurityUtils.getCurrentUser();
        if (user != null) {
            entity.setUserId(user.getUserId());
            entity.setUsername(user.getUsername());
        }
        entity.setModule(operationLog.module());
        entity.setOperation(operationLog.operation());
        // method 存 HTTP 请求方法(GET/POST/PUT/DELETE),供前端翻译成中文展示
        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setMethod(request.getMethod());
        }
        entity.setParams(buildParams(pjp.getArgs()));
        entity.setIp(resolveIp(request));
        entity.setCostMs((int) costMs);
        entity.setCreateTime(LocalDateTime.now());
        systemService.addLog(entity);
    }

    /** 序列化方法参数,过滤敏感字段并截断超长内容 */
    private String buildParams(Object[] args) {
        try {
            String params = objectMapper.writeValueAsString(args);
            params = params.replaceAll("(\"password\"\\s*:\\s*)\"[^\"]*\"", "$1\"***\"");
            return params.length() > 500 ? params.substring(0, 500) + "..." : params;
        } catch (Exception e) {
            return null;
        }
    }

    /** 获取当前 Web 请求(非 Web 线程返回 null) */
    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }

    /** 获取客户端 IP(优先 X-Forwarded-For) */
    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
