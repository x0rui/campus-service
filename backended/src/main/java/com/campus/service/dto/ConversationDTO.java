package com.campus.service.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConversationDTO {
    private Long otherUserId;
    private String otherNickName;
    private String otherAvatarUrl;
    private String lastMessage;
    private Integer lastMsgType;
    private LocalDateTime lastTime;
    private Integer unreadCount;
    private Boolean isOnline;
}
