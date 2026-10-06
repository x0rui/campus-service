package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Announcement implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String clubName;
    private String title;
    private String content;
    private String image;
    private String location;
    // 标签，逗号分隔（讲座/竞赛/学术活动/求助等），用于按兴趣推荐
    private String tags;
    private String eventTime;
    private Integer status;
    private Integer type;
    private Integer likeCount;
    private Integer commentCount;
    private Integer pinned;
    private LocalDateTime createTime;

    // 非数据库字段，用于展示发帖人信息
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String nickName;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String avatarUrl;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private Integer role;
}
