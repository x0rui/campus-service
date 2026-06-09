package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    //这三个是手写的查询
    @Select("SELECT * FROM user WHERE openid = #{openid}")
    User selectByOpenid(String openid); // 根据微信 openid 查询用户

    @Select("SELECT COUNT(*) FROM user WHERE role = 0 AND status = 0")
    Long countStudents(); // 统计普通学生数量

    @Select("SELECT COUNT(*) FROM user WHERE DATE(create_time) = CURDATE()")
    Long countTodayNewUsers();

    // 废弃：存在SQL注入风险，改用 Service 层 LambdaQueryWrapper
    @Select("SELECT DISTINCT u.* FROM user u WHERE u.user_id != #{userId} AND u.hobbies IS NOT NULL AND u.hobbies != '' AND u.hobbies LIKE CONCAT('%',#{keyword},'%') LIMIT 20")
    List<User> selectByHobbyKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);
}
