package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("orders")
public class Orders implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long orderId;
    private String orderNo;
    private Long goodsId;
    private Long buyerId;
    private Long sellerId;
    private BigDecimal amount;
    // 0待付款 1已付款 2待收货 3已完成 4已取消
    private Integer status;
    private String payChannel;
    private LocalDateTime payTime;
    private LocalDateTime finishTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // 列表展示用，不落库
    @TableField(exist = false)
    private String goodsTitle;
    @TableField(exist = false)
    private String goodsImage;
}
