package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.PostLike;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PostLikeMapper extends BaseMapper<PostLike> {

    @Select("SELECT COUNT(*) FROM post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int exists(@Param("postId") Long postId, @Param("userId") Long userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int deleteByPostAndUser(@Param("postId") Long postId, @Param("userId") Long userId);

    @org.apache.ibatis.annotations.Delete("DELETE FROM post_like WHERE post_id = #{postId}")
    int deleteByPostId(@Param("postId") Long postId);
}
