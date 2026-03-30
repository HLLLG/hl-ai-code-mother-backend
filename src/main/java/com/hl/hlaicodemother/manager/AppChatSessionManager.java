package com.hl.hlaicodemother.manager;

import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.model.dto.appChat.AppChatEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用协作对话 WebSocket 会话管理器
 */
@Component
@Slf4j
public class AppChatSessionManager {

    private final Map<Long, Map<String, WebSocketSession>> appSessionMap = new ConcurrentHashMap<>();

    private final Map<String, Long> sessionAppMap = new ConcurrentHashMap<>();

    public void register(Long appId, WebSocketSession session) {
        appSessionMap.computeIfAbsent(appId, key -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        sessionAppMap.put(session.getId(), appId);
    }

    public void unregister(String sessionId) {
        Long appId = sessionAppMap.remove(sessionId);
        if (appId == null) {
            return;
        }
        Map<String, WebSocketSession> sessionMap = appSessionMap.get(appId);
        if (sessionMap == null) {
            return;
        }
        sessionMap.remove(sessionId);
        if (sessionMap.isEmpty()) {
            appSessionMap.remove(appId);
        }
    }

    public Long getAppId(String sessionId) {
        return sessionAppMap.get(sessionId);
    }

    public void sendToSession(WebSocketSession session, AppChatEvent event) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(event)));
        } catch (IOException e) {
            log.warn("发送应用协作事件失败, sessionId: {}, eventType: {}", session.getId(), event.getEventType(), e);
        }
    }

    public void broadcast(Long appId, AppChatEvent event) {
        Map<String, WebSocketSession> sessionMap = appSessionMap.get(appId);
        if (sessionMap == null || sessionMap.isEmpty()) {
            return;
        }
        List<String> closedSessionIds = new ArrayList<>();
        for (Map.Entry<String, WebSocketSession> entry : sessionMap.entrySet()) {
            WebSocketSession session = entry.getValue();
            if (session == null || !session.isOpen()) {
                closedSessionIds.add(entry.getKey());
                continue;
            }
            try {
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(event)));
            } catch (IOException e) {
                closedSessionIds.add(entry.getKey());
                log.warn("广播应用协作事件失败, appId: {}, sessionId: {}, eventType: {}",
                        appId, entry.getKey(), event.getEventType(), e);
            }
        }
        closedSessionIds.forEach(this::unregister);
    }
}
