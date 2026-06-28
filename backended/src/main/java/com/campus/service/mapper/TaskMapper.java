package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Task;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT * FROM task WHERE status = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Task> selectPendingList(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM task WHERE status = 0 ORDER BY create_time DESC")
    List<Task> selectAllPending();

    @Select("SELECT * FROM task WHERE status = 0 AND task_type = #{taskType} ORDER BY create_time DESC")
    List<Task> selectByType(@Param("taskType") String taskType);

    @Select("SELECT * FROM task WHERE status = 0 AND (pickup_location LIKE CONCAT('%',#{keyword},'%') OR delivery_location LIKE CONCAT('%',#{keyword},'%') OR remark LIKE CONCAT('%',#{keyword},'%')) ORDER BY create_time DESC")
    List<Task> searchByKeyword(@Param("keyword") String keyword);

    // 更新任务状态为已接
    @Update("UPDATE task SET status = 1, taker_id = #{takerId}, take_time = NOW() WHERE task_id = #{taskId} AND status = 0")
    int takeTask(@Param("taskId") Long taskId, @Param("takerId") Long takerId);

    @Select("SELECT * FROM task WHERE publisher_id = #{userId} ORDER BY create_time DESC")
    List<Task> selectByPublisher(@Param("userId") Long userId);

    @Select("SELECT * FROM task WHERE taker_id = #{userId} ORDER BY create_time DESC")
    List<Task> selectByTaker(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM task WHERE status = 0")
    Long countPendingTasks();

    @Select("SELECT COUNT(*) FROM task WHERE DATE(create_time) = CURDATE()")
    Long countTodayTasks();

    @Select("SELECT DISTINCT task_type FROM task WHERE taker_id = #{userId} ORDER BY task_type")
    List<String> selectUserTakTypes(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM task WHERE taker_id = #{userId} AND DATE(take_time) = CURDATE() AND status != 3")
    Integer countTodayTakeByUser(@Param("userId") Long userId);

    @Update("UPDATE task SET status = 3 WHERE status = 0 AND deadline IS NOT NULL AND deadline < NOW()")
    int autoCancelExpired();

    // 截止时间到了，已接单但双方都没确认 → 自动完成
    @Update("UPDATE task SET status = 2, complete_time = NOW() WHERE status = 1 AND deadline IS NOT NULL AND deadline < NOW()")
    int autoCompleteExpired();

    // 接单者放弃任务，退回待接单状态
    @Update("UPDATE task SET status = 0, taker_id = NULL, take_time = NULL WHERE task_id = #{taskId} AND status = 1 AND taker_id = #{takerId}")
    int giveUpTask(@Param("taskId") Long taskId, @Param("takerId") Long takerId);

    @Select("SELECT COUNT(*) FROM task WHERE taker_id = #{userId} AND status = 2")
    int countCompletedByTaker(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM task WHERE taker_id = #{userId} AND status IN (1,2,3)")
    int countTakenByTaker(@Param("userId") Long userId);
}
