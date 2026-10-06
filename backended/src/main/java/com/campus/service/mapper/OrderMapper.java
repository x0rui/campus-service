package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Orders;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface OrderMapper extends BaseMapper<Orders> {

    // 我买的 + 我卖的
    @Select("SELECT * FROM orders WHERE buyer_id = #{userId} OR seller_id = #{userId} ORDER BY create_time DESC")
    List<Orders> selectMine(@Param("userId") Long userId);
}
