package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class Report {
    @TableId(type = IdType.AUTO)
    private Long reportId;
    private Long reporterId;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private String reason;
    private String description;
    private String evidenceImages;
    private Integer status;
    private Long handlerId;
    private String handleNote;
    private LocalDateTime handleTime;
    private LocalDateTime createTime;

    // 非数据库字段
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String reporterName;
}
