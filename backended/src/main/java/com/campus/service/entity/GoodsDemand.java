package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("goods_demand")
public class GoodsDemand implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long demandId;
    private Long userId;
    @NotBlank(message = "求购标题不能为空")
    private String title;
    private String category;
    private String keyword;
    private BigDecimal maxPrice;
    private String description;
    private Integer status;
    private Integer matchCount;
    private LocalDateTime createTime;
}
