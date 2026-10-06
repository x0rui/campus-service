package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("question_record")
public class QuestionRecord implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long recordId;
    private Long userId;
    private Long questionId;
    private String userAnswer;
    // 0答错/未判分 1答对（主观题恒0）
    private Integer isCorrect;
    private LocalDateTime createTime;
}
