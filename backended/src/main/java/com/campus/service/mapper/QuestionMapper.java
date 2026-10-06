package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Question;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface QuestionMapper extends BaseMapper<Question> {

    // 题库科目去重列表
    @Select("SELECT DISTINCT subject FROM question ORDER BY subject")
    List<String> selectSubjects();

    // 某用户错题本里的题目（错题本反向推荐资料要用到 subject/chapter）
    @Select("SELECT q.* FROM question q JOIN question_wrong w ON q.question_id = w.question_id " +
            "WHERE w.user_id = #{userId} ORDER BY w.update_time DESC")
    List<Question> selectWrongQuestions(@Param("userId") Long userId);
}
