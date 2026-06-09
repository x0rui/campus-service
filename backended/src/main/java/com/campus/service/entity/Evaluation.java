package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Data
@TableName("evaluation")
public class Evaluation {
    @TableId(type = IdType.AUTO)
    private Long evalId;
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;
    private Long evaluatorId;
    @NotNull(message = "被评价者ID不能为空")
    private Long targetId;
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分需在1-5之间")
    @Max(value = 5, message = "评分需在1-5之间")
    private Integer score;
    private String content;
    private Integer isAnonymous;
    private LocalDateTime createTime;
}
