package com.petadoption.websocket;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private final Map<Long, List<WebSocketSession>> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void addSession(Long userId, WebSocketSession session) {
        userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(session);
    }

    public void removeSession(Long userId, WebSocketSession session) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                userSessions.remove(userId);
            }
        }
    }

    public void sendToUser(Long userId, String type, Map<String, Object> data) {
        List<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions != null) {
            Map<String, Object> message = Map.of("type", type, "data", data);
            try {
                String json = objectMapper.writeValueAsString(message);
                TextMessage textMessage = new TextMessage(json);
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            System.err.println("Error sending message to user " + userId + ": " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error serializing message: " + e.getMessage());
            }
        }
    }

    public void broadcast(String type, Map<String, Object> data) {
        Map<String, Object> message = Map.of("type", type, "data", data);
        try {
            String json = objectMapper.writeValueAsString(message);
            TextMessage textMessage = new TextMessage(json);
            for (List<WebSocketSession> sessions : userSessions.values()) {
                for (WebSocketSession session : sessions) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(textMessage);
                        } catch (IOException e) {
                            System.err.println("Error broadcasting: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error serializing broadcast message: " + e.getMessage());
        }
    }
}
