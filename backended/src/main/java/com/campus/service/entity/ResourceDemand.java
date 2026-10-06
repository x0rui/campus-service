package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("resource_demand")
public class ResourceDemand implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long demandId;
    private Long userId;
    @NotBlank(message = "需求标题不能为空")
    private String title;
    private String course;
    private String resourceType;
    private String keyword;
    private String description;
    private Integer status;
    private Integer matchCount;
    private LocalDateTime createTime;
}
