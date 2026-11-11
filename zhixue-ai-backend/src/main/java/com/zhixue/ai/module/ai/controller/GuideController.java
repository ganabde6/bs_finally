package com.zhixue.ai.module.ai.controller;

import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.engine.AiServiceProvider;
import com.zhixue.ai.module.ai.service.ContentRiskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * AI 网站使用向导(免登录)
 * <p>全站悬浮机器人入口调用,回答网站功能与操作引导问题</p>
 * <p>安全设计:IP 限频(每分钟 10 次) + 提问内容风控检测</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideController {

    private final AiServiceProvider aiServiceProvider;
    private final ContentRiskService contentRiskService;

    /** 单个 IP 每分钟最大请求数 */
    private static final int MAX_PER_MINUTE = 10;
    /** 问题最大长度 */
    private static final int MAX_QUESTION_LENGTH = 500;

    /** 简单内存限频:IP -> 最近请求时间戳队列 */
    private final Map<String, Deque<Long>> rateLimiter = new ConcurrentHashMap<>();

    /**
     * AI 向导对话
     * @param body {question: 用户提问}
     */
    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        String question = body == null ? null : (String) body.get("question");
        if (question == null || question.trim().isEmpty()) {
            return Result.error("请输入您的问题");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            return Result.error("问题过长,请保持在 " + MAX_QUESTION_LENGTH + " 字以内");
        }
        String ip = clientIp(request);
        if (!tryAcquire(ip)) {
            return Result.error("提问太频繁,请稍后再试");
        }
        // 内容风控:检测用户提问,违规自动拦截并记录
        contentRiskService.audit(question, "AI 向导");
        return Result.success(aiServiceProvider.guideAnswer(question));
    }

    /** 滑动窗口限频:1 分钟内同一 IP 最多 MAX_PER_MINUTE 次 */
    private boolean tryAcquire(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> window = rateLimiter.computeIfAbsent(ip, k -> new ConcurrentLinkedDeque<>());
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() > 60_000L) {
                window.pollFirst();
            }
            if (window.size() >= MAX_PER_MINUTE) {
                return false;
            }
            window.addLast(now);
            return true;
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
