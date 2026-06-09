package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface CommentMapper extends BaseMapper<Comment> {

    @Select("SELECT c.*, u.nick_name AS nickName, u.avatar_url AS avatarUrl, u.role " +
            "FROM comment c LEFT JOIN user u ON c.user_id = u.user_id " +
            "WHERE c.post_id = #{postId} ORDER BY c.pinned DESC, c.create_time ASC")
    List<Comment> selectByPostId(@Param("postId") Long postId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM comment WHERE post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);

    @org.apache.ibatis.annotations.Update("UPDATE comment SET like_count = like_count + 1 WHERE id = #{id}")
    int incrLikeCount(@Param("id") Long id);

    @org.apache.ibatis.annotations.Update("UPDATE comment SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{id}")
    int decrLikeCount(@Param("id") Long id);

    @Select("SELECT * FROM comment WHERE reply_to = #{replyTo}")
    List<Comment> selectByReplyTo(@Param("replyTo") Long replyTo);
}
