package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.TeamJoin;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface TeamJoinMapper extends BaseMapper<TeamJoin> {

    @Select("SELECT * FROM team_join WHERE team_id = #{teamId} AND status = 1")
    List<TeamJoin> selectMembers(@Param("teamId") Long teamId);

    @Select("SELECT * FROM team_join WHERE user_id = #{userId} AND status = 1 ORDER BY create_time DESC")
    List<TeamJoin> selectByUserId(@Param("userId") Long userId);

    @org.apache.ibatis.annotations.Update("UPDATE team_join SET status = 2 WHERE team_id = #{teamId} AND status = 0")
    int rejectAllPending(@Param("teamId") Long teamId);
}
