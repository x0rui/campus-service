package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User implements Serializable {
    private static final long serialVersionUID = 839524642830358298L;
    @TableId(type = IdType.AUTO)
    private Long userId;
    private String openid;
    private String nickName;
    private String avatarUrl;
    private Integer role;
    private String realName;
    private String studentId;
    private String phone;
    private String college;
    private String major;
    private String className;
    private Integer age;
    private String gender;
    private String hobbies;
    private Integer status;
    private Integer creditScore;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
