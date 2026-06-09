package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Report;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.time.LocalDateTime;
import java.util.List;

public interface ReportMapper extends BaseMapper<Report> {

    @Select("SELECT r.*, u.nick_name AS reporterName FROM report r " +
            "LEFT JOIN user u ON r.reporter_id = u.user_id " +
            "WHERE r.reporter_id = #{userId} ORDER BY r.create_time DESC")
    List<Report> selectByReporter(@Param("userId") Long userId);

    @Select("SELECT r.*, u.nick_name AS reporterName FROM report r " +
            "LEFT JOIN user u ON r.reporter_id = u.user_id " +
            "WHERE r.status = #{status} ORDER BY r.create_time DESC")
    List<Report> selectByStatus(@Param("status") int status);

    @Select("SELECT r.*, u.nick_name AS reporterName FROM report r " +
            "LEFT JOIN user u ON r.reporter_id = u.user_id " +
            "WHERE r.target_type = #{targetType} AND r.status = #{status} ORDER BY r.create_time DESC")
    List<Report> selectByTypeAndStatus(@Param("targetType") String targetType, @Param("status") int status);

    @Update("UPDATE report SET status = #{status}, handler_id = #{handlerId}, " +
            "handle_note = #{handleNote}, handle_time = #{handleTime} WHERE report_id = #{reportId}")
    int handleReport(@Param("reportId") Long reportId, @Param("status") int status,
                     @Param("handlerId") Long handlerId, @Param("handleNote") String handleNote,
                     @Param("handleTime") LocalDateTime handleTime);
}
