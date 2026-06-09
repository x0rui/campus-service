package com.campus.service.service;

import com.campus.service.dto.ConversationDTO;
import com.campus.service.entity.Message;
import com.campus.service.entity.User;
import com.campus.service.mapper.MessageMapper;
import com.campus.service.mapper.UserMapper;
import com.campus.service.websocket.ChatWebSocketHandler;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    public ChatService(MessageMapper messageMapper, UserMapper userMapper) {
        this.messageMapper = messageMapper;
        this.userMapper = userMapper;
    }

    // 构建会话ID，确保用户A和用户B的会话ID是相同的
    public static String buildSessionId(Long userA, Long userB) {
        return userA < userB ? userA + "_" + userB : userB + "_" + userA;
    }

    // 发送消息，返回发送成功的消息
    public Message sendMessage(Message message) {
        message.setIsRead(0);
        if (message.getSessionId() == null || message.getSessionId().isEmpty()) {
            message.setSessionId(buildSessionId(message.getSenderId(), message.getReceiverId()));
        }
        messageMapper.insert(message);
        return message;
    }

    public List<Message> getMessages(String sessionId) {
        return messageMapper.selectRecentBySessionId(sessionId);
    }

    public int markAsRead(String sessionId, Long userId) {
        return messageMapper.markAsRead(sessionId, userId);
    }

    public Integer getUnreadCount(Long userId) {
        return messageMapper.countUnread(userId);
    }

    public List<Message> getRecentMessages(String sessionId) {
        return messageMapper.selectRecentBySessionId(sessionId);
    }

    public List<String> getUserSessions(Long userId) {
        return messageMapper.selectUserSessions(userId);
    }

    public List<ConversationDTO> getConversations(Long userId) {
        List<String> sessions = messageMapper.selectUserSessions(userId);
        List<ConversationDTO> result = new ArrayList<>();
        for (String sessionId : sessions) {
            String[] parts = sessionId.split("_");
            Long otherId = parts[0].equals(String.valueOf(userId)) ? Long.valueOf(parts[1]) : Long.valueOf(parts[0]);

            Message lastMsg = messageMapper.selectLastBySessionId(sessionId);
            Integer unread = messageMapper.countUnreadBySession(sessionId, userId);

            ConversationDTO dto = new ConversationDTO();
            dto.setOtherUserId(otherId);
            dto.setLastMessage(lastMsg != null ? lastMsg.getContent() : "");
            dto.setLastMsgType(lastMsg != null ? lastMsg.getMsgType() : 0);
            dto.setLastTime(lastMsg != null ? lastMsg.getCreateTime() : null);
            dto.setUnreadCount(unread != null ? unread : 0);
            dto.setIsOnline(ChatWebSocketHandler.isUserOnline(otherId));

            User otherUser = userMapper.selectById(otherId);
            if (otherUser != null) {
                dto.setOtherNickName(otherUser.getNickName());
                dto.setOtherAvatarUrl(otherUser.getAvatarUrl());
            } else {
                dto.setOtherNickName("微信用户");
                dto.setOtherAvatarUrl(null);
            }
            result.add(dto);
        }
        return result;
    }
}
