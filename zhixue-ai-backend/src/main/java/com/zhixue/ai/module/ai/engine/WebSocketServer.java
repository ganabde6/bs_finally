package com.zhixue.ai.module.ai.engine;

import com.alibaba.fastjson2.JSON;
import com.zhixue.ai.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 实时推送服务端
 * <p>用于:批改结果推送、考试状态同步、消息通知</p>
 * <p>连接地址:ws://host:8080/ws/{userId}</p>
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {

    /** 在线会话(userId -> session) */
    private static final Map<Long, Session> SESSIONS = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(@PathParam("userId") Long userId, Session session) {
        SESSIONS.put(userId, session);
        log.info("WebSocket 连接建立: userId={}, 在线人数={}", userId, SESSIONS.size());
    }

    @OnClose
    public void onClose(@PathParam("userId") Long userId, Session session) {
        SESSIONS.remove(userId);
        log.info("WebSocket 连接关闭: userId={}, 在线人数={}", userId, SESSIONS.size());
    }

    @OnError
    public void onError(@PathParam("userId") Long userId, Session session, Throwable error) {
        log.error("WebSocket 错误: userId={}", userId, error);
        SESSIONS.remove(userId);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到 WebSocket 消息: {}", message);
    }

    /**
     * 推送消息给指定用户
     */
    public static void sendToUser(Long userId, Object data) {
        Session session = SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(JSON.toJSONString(Result.success(data, "实时推送")));
            } catch (IOException e) {
                log.error("推送消息失败: userId={}", userId, e);
            }
        }
    }

    /**
     * 广播给所有在线用户
     */
    public static void broadcast(Object data) {
        SESSIONS.forEach((uid, session) -> {
            if (session.isOpen()) {
                try {
                    session.getBasicRemote().sendText(JSON.toJSONString(Result.success(data, "广播通知")));
                } catch (IOException e) {
                    log.error("广播失败: userId={}", uid, e);
                }
            }
        });
    }

    public static int getOnlineCount() {
        return SESSIONS.size();
    }
}
