package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("question")
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long questionId;
    private String subject;
    private String chapter;
    // 题型: 0单选 1多选 2判断 3填空 4主观
    // 必须显式指定：Jackson 默认会把 qType 序列化成 "qtype"，前端读的是 qType，会拿不到值
    @JsonProperty("qType")
    private Integer qType;
    private String content;
    // 选项 JSON 数组
    private String options;
    private String answer;
    private String analysis;
    private LocalDateTime createTime;
}
