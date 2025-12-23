package com.petadoption.websocket;

import com.petadoption.entity.User;
import com.petadoption.repository.UserRepository;
import com.petadoption.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private final Map<WebSocketSession, Long> sessionUserMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = extractToken(session);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        String email = jwtTokenProvider.getEmailFromToken(token);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }

        sessionUserMap.put(session, user.getId());
        notificationService.addSession(user.getId(), session);
        System.out.println("User " + user.getId() + " connected via WebSocket");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // Handle ping/pong
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
            return;
        }

        // Handle other messages if needed
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = sessionUserMap.remove(session);
        if (userId != null) {
            notificationService.removeSession(userId, session);
            System.out.println("User " + userId + " disconnected from WebSocket");
        }
    }

    private String extractToken(WebSocketSession session) {
        String uri = session.getUri() != null ? session.getUri().toString() : "";
        // Token can be in path: /ws/{token} or query param: /ws?token=xxx
        if (uri.contains("/ws/")) {
            String[] parts = uri.split("/ws/");
            if (parts.length > 1) {
                String tokenPart = parts[1];
                // Remove query string if present
                if (tokenPart.contains("?")) {
                    tokenPart = tokenPart.substring(0, tokenPart.indexOf("?"));
                }
                return tokenPart;
            }
        }
        // Try query parameter
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    return param.substring(6);
                }
            }
        }
        return null;
    }
}
