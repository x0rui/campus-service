package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Evaluation;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface EvaluationMapper extends BaseMapper<Evaluation> {

    @Select("SELECT * FROM evaluation WHERE target_id = #{userId} ORDER BY create_time DESC")
    List<Evaluation> selectByTargetId(@Param("userId") Long userId);

    @Select("SELECT * FROM evaluation WHERE evaluator_id = #{userId} ORDER BY create_time DESC")
    List<Evaluation> selectByEvaluatorId(@Param("userId") Long userId);

    @Select("SELECT AVG(score) FROM evaluation WHERE target_id = #{userId}")
    Double getAvgScore(@Param("userId") Long userId);

    @Select("SELECT target_id, AVG(score) as avg_score, COUNT(*) as count FROM evaluation WHERE order_type IN (0,1) GROUP BY target_id HAVING count >= 1 ORDER BY avg_score DESC LIMIT 20")
    List<java.util.Map<String, Object>> selectTopUsers();

    @Select("SELECT t.team_id, t.title, AVG(e.score) as avg_score, COUNT(*) as count FROM evaluation e JOIN team t ON e.order_id = t.team_id WHERE e.order_type = 2 GROUP BY t.team_id, t.title ORDER BY avg_score DESC LIMIT 20")
    List<java.util.Map<String, Object>> selectTopTeams();

    @Select("SELECT ca.app_id, ca.club_name, AVG(e.score) as avg_score, COUNT(*) as count FROM evaluation e JOIN club_application ca ON e.order_id = ca.app_id WHERE e.order_type = 3 AND ca.status = 1 GROUP BY ca.app_id, ca.club_name ORDER BY avg_score DESC LIMIT 20")
    List<java.util.Map<String, Object>> selectTopClubs();

    @Select("SELECT COUNT(*) FROM evaluation WHERE order_id = #{orderId} AND order_type = #{orderType} AND evaluator_id = #{userId}")
    int countByOrder(@Param("orderId") Long orderId, @Param("orderType") Integer orderType, @Param("userId") Long userId);
}
