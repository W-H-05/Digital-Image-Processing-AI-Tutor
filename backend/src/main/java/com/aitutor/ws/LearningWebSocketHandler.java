package com.aitutor.ws;

import com.aitutor.common.Constants;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 学情实时推送处理器：教师端连接后接收实时动态
 */
@Slf4j
@Component
public class LearningWebSocketHandler extends TextWebSocketHandler {

    private final Map<Long, WebSocketSession> teacherSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // token 由前端握手时通过查询参数携带；这里简化：从 session 属性拿 userId（由拦截器注入）
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }
        teacherSessions.put(userId, session);
        log.info("教师 {} 建立学情 WebSocket 连接，当前连接数 {}", userId, teacherSessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        teacherSessions.entrySet().removeIf(e -> e.getValue().equals(session));
    }

    /**
     * 广播实时动态给所有在线教师
     */
    public void broadcast(Map<String, Object> message) {
        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            return;
        }
        TextMessage tm = new TextMessage(json);
        teacherSessions.values().forEach(s -> {
            try {
                if (s.isOpen()) {
                    synchronized (s) {
                        s.sendMessage(tm);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }
}
