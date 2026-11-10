package com.zhixue.ai.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.common.result.ResultCode;
import com.zhixue.ai.module.ai.engine.AiServiceProvider;
import com.zhixue.ai.module.ai.entity.ContentRiskLog;
import com.zhixue.ai.module.ai.mapper.ContentRiskLogMapper;
import com.zhixue.ai.security.LoginUser;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

/**
 * 内容风控服务:教师/学生提交内容时自动检测,
 * 违规自动拦截并记录到 content_risk_log,供管理端风控页查看
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentRiskService {

    private final AiServiceProvider aiServiceProvider;
    private final ContentRiskLogMapper riskLogMapper;

    /**
     * 检测内容,违规则拦截并自动记录;检测异常时不阻断业务(fail-open)
     * @param content 待检测文本内容
     * @param source  内容入口(如"新增题目"、"提交作答")
     */
    public void audit(String content, String source) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        String risk;
        try {
            risk = aiServiceProvider.moderateContent(content);
        } catch (Exception e) {
            log.warn("内容风控检测异常,放行: {}", e.getMessage());
            return;
        }
        if (risk != null) {
            record(content, source, risk);
            throw new BizException(ResultCode.CONTENT_RISK_BLOCK.getCode(), risk);
        }
    }

    /** 分页查询内容风控记录(管理端) */
    public Page<ContentRiskLog> pageContentRisks(Long current, Long size) {
        return riskLogMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ContentRiskLog>()
                        .orderByDesc(ContentRiskLog::getCreateTime)
                        .orderByDesc(ContentRiskLog::getId));
    }

    /** 记录一条违规内容风控日志(记录失败不影响拦截) */
    private void record(String content, String source, String risk) {
        try {
            ContentRiskLog entry = new ContentRiskLog();
            LoginUser user = SecurityUtils.getCurrentUser();
            if (user != null) {
                entry.setUserId(user.getUserId());
                entry.setUsername(user.getUsername());
                entry.setRoleCode(user.getRoleCode());
            }
            entry.setSource(source);
            entry.setContent(content.length() > 2000 ? content.substring(0, 2000) + "..." : content);
            entry.setRiskReason(risk.length() > 190 ? risk.substring(0, 190) : risk);
            entry.setIp(resolveIp());
            entry.setCreateTime(LocalDateTime.now());
            riskLogMapper.insert(entry);
        } catch (Exception e) {
            log.warn("内容风控日志记录失败: {}", e.getMessage());
        }
    }

    /** 获取客户端 IP(优先 X-Forwarded-For) */
    private String resolveIp() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
