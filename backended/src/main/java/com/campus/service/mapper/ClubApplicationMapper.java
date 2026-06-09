package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.ClubApplication;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface ClubApplicationMapper extends BaseMapper<ClubApplication> {

    @Select("SELECT * FROM club_application WHERE status = 0 ORDER BY create_time DESC")
    List<ClubApplication> selectPendingList();

    @Select("SELECT * FROM club_application WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<ClubApplication> selectByUserId(@Param("userId") Long userId);
}
