package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`resource`")
public class Resource implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long resourceId;
    private Long userId;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    @NotBlank(message = "课程不能为空")
    private String course;
    @NotBlank(message = "资料类型不能为空")
    private String resourceType;
    private String fileName;
    @NotBlank(message = "文件不能为空")
    private String fileUrl;
    private String fileExt;
    private Long fileSize;
    private Integer viewCount;
    private Integer downloadCount;
    private Integer collectCount;
    private BigDecimal score;
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime auditTime;
}
