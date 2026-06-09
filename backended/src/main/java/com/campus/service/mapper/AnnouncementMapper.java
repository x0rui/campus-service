package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Announcement;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface AnnouncementMapper extends BaseMapper<Announcement> {

    @Select("SELECT a.*, u.nick_name AS nickName, u.avatar_url AS avatarUrl, u.role " +
            "FROM announcement a LEFT JOIN user u ON a.user_id = u.user_id " +
            "WHERE a.status = 0 ORDER BY a.create_time DESC LIMIT #{limit}")
    List<Announcement> selectLatest(@Param("limit") int limit);

    @Select("SELECT a.*, u.nick_name AS nickName, u.avatar_url AS avatarUrl, u.role " +
            "FROM announcement a LEFT JOIN user u ON a.user_id = u.user_id " +
            "WHERE a.status = 0 AND a.type = #{type} ORDER BY a.pinned DESC, a.create_time DESC")
    List<Announcement> selectByType(@Param("type") int type);

    @Select("SELECT a.*, u.nick_name AS nickName, u.avatar_url AS avatarUrl, u.role " +
            "FROM announcement a LEFT JOIN user u ON a.user_id = u.user_id " +
            "WHERE a.status = 0 AND a.type = #{type} ORDER BY a.pinned DESC, a.create_time DESC " +
            "LIMIT #{offset}, #{limit}")
    List<Announcement> selectByTypePage(@Param("type") int type, @Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT a.*, u.nick_name AS nickName, u.avatar_url AS avatarUrl, u.role " +
            "FROM announcement a LEFT JOIN user u ON a.user_id = u.user_id " +
            "WHERE a.id = #{id}")
    @Override
    Announcement selectById(@Param("id") java.io.Serializable id);

    @org.apache.ibatis.annotations.Update("UPDATE announcement SET like_count = GREATEST(like_count + 1, 0) WHERE id = #{id}")
    int incrLikeCount(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE announcement SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrLikeCount(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE announcement SET comment_count = comment_count + 1 WHERE id = #{id}")
    int incrCommentCount(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE announcement SET comment_count = GREATEST(comment_count - 1, 0) WHERE id = #{id}")
    int decrCommentCount(@Param("id") Long id);
}
