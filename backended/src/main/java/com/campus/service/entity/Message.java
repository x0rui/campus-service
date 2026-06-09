package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("message")
public class Message {
    @TableId(type = IdType.AUTO)
    private Long msgId;
    private String sessionId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer msgType;
    private Integer isRead;
    private LocalDateTime createTime;
}
