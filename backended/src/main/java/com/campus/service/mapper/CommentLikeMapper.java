package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.CommentLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Select("SELECT COUNT(1) FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int exists(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM comment_like WHERE comment_id = #{commentId} AND user_id = #{userId}")
    int deleteByCommentAndUser(@Param("commentId") Long commentId, @Param("userId") Long userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM comment_like WHERE comment_id IN (SELECT id FROM comment WHERE post_id = #{postId})")
    int deleteByPostId(@Param("postId") Long postId);
}
