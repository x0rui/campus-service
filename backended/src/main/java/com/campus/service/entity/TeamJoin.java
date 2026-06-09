package com.campus.service.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("team_join")
public class TeamJoin {
    @TableId(type = IdType.AUTO)
    private Long joinId;
    private Long teamId;
    private Long userId;
    private Integer status;
    private Integer checkedIn;
    private LocalDateTime createTime;
}
