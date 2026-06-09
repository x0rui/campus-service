package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Team;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface TeamMapper extends BaseMapper<Team> {

    @Select("SELECT * FROM team WHERE status = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Team> selectActiveList(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM team WHERE status = 0 AND tag = #{tag} ORDER BY create_time DESC")
    List<Team> selectByTag(@Param("tag") String tag);

    @Select("SELECT * FROM team WHERE status = 0 AND (title LIKE CONCAT('%',#{keyword},'%') OR description LIKE CONCAT('%',#{keyword},'%') OR location LIKE CONCAT('%',#{keyword},'%')) ORDER BY create_time DESC")
    List<Team> searchByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM team WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Team> selectByUserId(@Param("userId") Long userId);

    @Update("UPDATE team SET current_members = current_members + 1 WHERE team_id = #{teamId} AND current_members < max_members")
    int incrMember(@Param("teamId") Long teamId);

    @Update("UPDATE team SET current_members = current_members - 1 WHERE team_id = #{teamId} AND current_members > 1")
    int decrMember(@Param("teamId") Long teamId);

    @Update("UPDATE team SET status = 1 WHERE team_id = #{teamId} AND current_members >= max_members")
    int autoFull(@Param("teamId") Long teamId);

    @Update("UPDATE team SET status = 2 WHERE status IN (0,1) AND end_time IS NOT NULL AND end_time < NOW() AND current_members >= min_members")
    int autoCloseExpired();

    @Update("UPDATE team SET status = 3 WHERE status IN (0,1) AND end_time IS NOT NULL AND end_time < NOW() AND current_members < min_members")
    int autoCancelExpired();
}
