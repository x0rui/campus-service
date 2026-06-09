package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.ClubMember;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ClubMemberMapper extends BaseMapper<ClubMember> {

    @Select("SELECT cm.*, u.nick_name AS nickName, u.avatar_url AS avatarUrl, u.gender AS gender, u.age AS age, u.hobbies AS hobbies, u.college AS college FROM club_member cm " +
            "LEFT JOIN user u ON cm.user_id = u.user_id WHERE cm.club_id = #{clubId} ORDER BY cm.create_time ASC")
    List<ClubMember> selectByClubId(@Param("clubId") Long clubId);

    @Select("SELECT COUNT(1) FROM club_member WHERE club_id = #{clubId} AND user_id = #{userId} AND status = 1")
    int isMember(@Param("clubId") Long clubId, @Param("userId") Long userId);
}
