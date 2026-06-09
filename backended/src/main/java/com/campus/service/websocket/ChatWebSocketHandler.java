package com.campus.service.websocket;

import com.campus.service.entity.Message;
import com.campus.service.service.ChatService;
import com.campus.service.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();
    private final ChatService chatService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtUtil jwtUtil;

    public ChatWebSocketHandler(ChatService chatService, JwtUtil jwtUtil) {
        this.chatService = chatService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineUsers.put(userId, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws IOException {
        Long senderId = getUserId(session);
        if (senderId == null) return;

        Map<String, Object> msgData = objectMapper.readValue(textMessage.getPayload(), Map.class);
        Long receiverId = msgData.get("receiverId") != null ? Long.valueOf(msgData.get("receiverId").toString()) : null;
        String content = msgData.get("content") != null ? msgData.get("content").toString() : "";
        Integer msgType = msgData.get("msgType") != null ? Integer.valueOf(msgData.get("msgType").toString()) : 0;

        if (receiverId == null || content.isEmpty()) return;
        if (senderId.equals(receiverId)) return; // 不能给自己发消息

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setSessionId(ChatService.buildSessionId(senderId, receiverId));
        chatService.sendMessage(message);

        WebSocketSession receiverSession = onlineUsers.get(receiverId);
        if (receiverSession != null && receiverSession.isOpen()) {
            Map<String, Object> pushData = new ConcurrentHashMap<>();
            pushData.put("senderId", senderId);
            pushData.put("content", content);
            pushData.put("msgType", msgType);
            pushData.put("createTime", message.getCreateTime() != null ? message.getCreateTime().toString() : "");
            receiverSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(pushData)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = getUserId(session);
        if (userId != null) {
            onlineUsers.remove(userId);
        }
    }

    public static boolean isUserOnline(Long userId) {
        WebSocketSession session = onlineUsers.get(userId);
        return session != null && session.isOpen();
    }

    public static void pushToUser(Long userId, Message message) {
        WebSocketSession session = onlineUsers.get(userId);
        if (session != null && session.isOpen()) {
            try {
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("senderId", message.getSenderId());
                data.put("content", message.getContent());
                data.put("msgType", message.getMsgType() != null ? message.getMsgType() : 0);
                data.put("createTime", message.getCreateTime() != null ? message.getCreateTime().toString() : "");
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
                session.sendMessage(new TextMessage(json));
            } catch (Exception ignored) {}
        }
    }

    private Long getUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String path = uri.getPath();
        String query = uri.getQuery();
        try {
            String[] parts = path.split("/");
            Long pathUserId = Long.valueOf(parts[parts.length - 1]);
            // 从查询参数中提取token并验证
            if (query != null && query.contains("token=")) {
                String token = query.substring(query.indexOf("token=") + 6);
                if (token.contains("&")) token = token.substring(0, token.indexOf("&"));
                if (!jwtUtil.isTokenExpired(token)) {
                    Long tokenUserId = jwtUtil.getUserIdFromToken(token);
                    // token中的userId必须和路径中的userId一致
                    if (tokenUserId != null && tokenUserId.equals(pathUserId)) {
                        return pathUserId;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }
}
