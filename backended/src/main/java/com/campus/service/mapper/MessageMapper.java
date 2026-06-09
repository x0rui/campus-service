package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface MessageMapper extends BaseMapper<Message> {

    @Select("SELECT * FROM message WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<Message> selectBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT * FROM (SELECT * FROM message WHERE session_id = #{sessionId} ORDER BY create_time DESC LIMIT 50) t ORDER BY create_time ASC")
    List<Message> selectRecentBySessionId(@Param("sessionId") String sessionId);

    @Update("UPDATE message SET is_read = 1 WHERE session_id = #{sessionId} AND receiver_id = #{userId} AND is_read = 0")
    int markAsRead(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM message WHERE receiver_id = #{userId} AND is_read = 0")
    Integer countUnread(@Param("userId") Long userId);

    @Select("SELECT DISTINCT session_id FROM message WHERE sender_id = #{userId} OR receiver_id = #{userId} ORDER BY (SELECT MAX(create_time) FROM message m2 WHERE m2.session_id = message.session_id) DESC")
    List<String> selectUserSessions(@Param("userId") Long userId);

    @Select("SELECT * FROM message WHERE session_id = #{sessionId} ORDER BY create_time DESC LIMIT 1")
    Message selectLastBySessionId(@Param("sessionId") String sessionId);

    @Select("SELECT COUNT(*) FROM message WHERE session_id = #{sessionId} AND receiver_id = #{userId} AND is_read = 0")
    Integer countUnreadBySession(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
