package com.campus.service.controller;

import com.campus.service.dto.Result;
import com.campus.service.dto.ConversationDTO;
import com.campus.service.entity.Message;
import com.campus.service.service.ChatService;
import com.campus.service.websocket.ChatWebSocketHandler;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 获取与某人的会话ID
     */
    @GetMapping("/session/{otherUserId}")
    public Result<String> getSessionId(HttpServletRequest request, @PathVariable Long otherUserId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        if (userId.equals(otherUserId)) {
            return Result.fail("不能和自己聊天");
        }
        return Result.ok(ChatService.buildSessionId(userId, otherUserId));
    }

    /**
     * 获取会话消息列表
     */
    @GetMapping("/messages/{sessionId}")
    public Result<List<Message>> getMessages(HttpServletRequest request, @PathVariable String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        // 验证会话归属：只有会话参与者才能读消息
        String[] parts = sessionId.split("_");
        if (parts.length != 2) return Result.fail("无效会话");
        Long id1 = Long.valueOf(parts[0]), id2 = Long.valueOf(parts[1]);
        if (!userId.equals(id1) && !userId.equals(id2)) return Result.fail(403, "无权查看");
        return Result.ok(chatService.getMessages(sessionId));
    }

    /**
     * 标记已读
     */
    @PutMapping("/read/{sessionId}")
    public Result<?> markAsRead(HttpServletRequest request, @PathVariable String sessionId) {
        Long userId = (Long) request.getAttribute("userId");
        chatService.markAsRead(sessionId, userId);
        return Result.ok();
    }

    /**
     * 发送消息（HTTP方式，作为WebSocket降级方案）
     */
    @PostMapping("/send")
    public Result<Message> sendMessage(HttpServletRequest request, @RequestBody Message message) {
        Long userId = (Long) request.getAttribute("userId");
        if (message.getReceiverId() == null) return Result.fail("接收者不能为空");
        if (userId.equals(message.getReceiverId())) return Result.fail("不能给自己发消息");
        message.setSenderId(userId);
        message.setSessionId(ChatService.buildSessionId(userId, message.getReceiverId()));
        chatService.sendMessage(message);
        ChatWebSocketHandler.pushToUser(message.getReceiverId(), message);
        return Result.ok(message);
    }

    /**
     * 获取未读消息数
     */
    @GetMapping("/unread")
    public Result<Integer> getUnreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.ok(0);
        return Result.ok(chatService.getUnreadCount(userId));
    }

    /**
     * 获取最近会话列表
     */
    @GetMapping("/sessions")
    public Result<List<String>> getSessions(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail("请先登录");
        return Result.ok(chatService.getUserSessions(userId));
    }

    /**
     * 获取会话列表（含最后消息、未读数、用户信息、在线状态）
     */
    @GetMapping("/conversations")
    public Result<List<ConversationDTO>> getConversations(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail("请先登录");
        return Result.ok(chatService.getConversations(userId));
    }

    /**
     * 检查用户是否在线
     */
    @GetMapping("/online/{userId}")
    public Result<Boolean> isOnline(@PathVariable Long userId) {
        return Result.ok(ChatWebSocketHandler.isUserOnline(userId));
    }
}
