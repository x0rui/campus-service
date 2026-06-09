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
@TableName("goods")
public class Goods implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long goodsId;
    private Long userId;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String description;
    @NotBlank(message = "分类不能为空")
    private String category;
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0.01", message = "价格不能小于0.01")
    private BigDecimal price;
    private String images;
    private Integer status;
    private Long buyerId;
    private Integer browseCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
