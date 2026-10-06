package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("payment_log")
public class PaymentLog implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long orderId;
    private String orderNo;
    private String prepayId;
    private String channel;
    private BigDecimal amount;
    // 0已创建 1成功 2失败
    private Integer status;
    private String tradeNo;
    private String notifyBody;
    private LocalDateTime createTime;
}
