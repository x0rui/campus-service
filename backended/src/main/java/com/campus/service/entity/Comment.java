package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
    private String content;
    private Long replyTo;
    private Integer pinned;
    private Integer likeCount;
    private LocalDateTime createTime;

    // 非数据库字段，用于展示用户信息
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String nickName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String avatarUrl;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer role;
}
