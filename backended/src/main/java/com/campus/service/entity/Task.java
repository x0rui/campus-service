package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import javax.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long taskId;
    private Long publisherId;
    private Long takerId;
    @NotBlank(message = "取件地点不能为空")
    private String pickupLocation;
    private BigDecimal pickupLat;
    private BigDecimal pickupLng;
    @NotBlank(message = "送达地点不能为空")
    private String deliveryLocation;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;
    @NotNull(message = "跑腿费不能为空")
    @DecimalMin(value = "0.01", message = "跑腿费不能小于0.01")
    @DecimalMax(value = "100.00", message = "跑腿费不能超过100")
    private BigDecimal fee;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime deadline;
    private String remark;
    private String taskType;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime takeTime;
    private LocalDateTime completeTime;
}
