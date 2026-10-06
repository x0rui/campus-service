package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("club_application")
public class ClubApplication {
    @TableId(type = IdType.AUTO)
    private Long appId;
    private Long userId;
    private String clubName;
    private String description;
    // 社团分类：学术/文艺/体育/公益/其他（支持学术类社团）
    private String category;
    private Integer status;
    private String reason;
    private LocalDateTime createTime;
    private LocalDateTime auditTime;
}
