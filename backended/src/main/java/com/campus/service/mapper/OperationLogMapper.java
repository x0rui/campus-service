package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.OperationLog;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface OperationLogMapper extends BaseMapper<OperationLog> {

    @Select("SELECT * FROM operation_log ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<OperationLog> selectPageDesc(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT * FROM operation_log WHERE operation LIKE CONCAT('%',#{keyword},'%') ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<OperationLog> searchByKeyword(@Param("keyword") String keyword, @Param("offset") int offset, @Param("limit") int limit);
}
