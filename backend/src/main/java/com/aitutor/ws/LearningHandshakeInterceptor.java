package com.aitutor.ws;

import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * WebSocket 握手拦截器：从查询参数 token 解析用户并注入 session 属性
 */
@Slf4j
@Component
public class LearningHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String query = request.getURI().getQuery();
            String token = UriComponentsBuilder.fromUri(request.getURI())
                    .build().getQueryParams().getFirst("token");
            if (token == null || token.isBlank()) {
                return false;
            }
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return false;
            }
            attributes.put("userId", Long.valueOf(loginId.toString()));
            return true;
        } catch (Exception e) {
            log.warn("WebSocket 握手鉴权失败", e);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
