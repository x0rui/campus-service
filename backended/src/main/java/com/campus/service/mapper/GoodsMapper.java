package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Goods;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
/*
继承 BaseMapper → 自带 insert/deleteById/selectById/selectList/updateById → 不用写 SQL
 */
public interface GoodsMapper extends BaseMapper<Goods> {

    // 获取上架商品列表
    @Select("SELECT * FROM goods WHERE status = 0 ORDER BY create_time DESC LIMIT #{offset}, #{limit}")
    List<Goods> selectOnSaleList(@Param("offset") int offset, @Param("limit") int limit);
    // 获取所有上架商品
    @Select("SELECT * FROM goods WHERE status = 0 ORDER BY create_time DESC")
    List<Goods> selectAllOnSale();
    // 根据分类获取商品列表
    @Select("SELECT * FROM goods WHERE status = 0 AND category = #{category} ORDER BY create_time DESC")
    List<Goods> selectByCategory(@Param("category") String category);
    // 搜索商品
    @Select("SELECT * FROM goods WHERE status = 0 AND title LIKE CONCAT('%',#{keyword},'%') ORDER BY create_time DESC")
    List<Goods> searchByKeyword(@Param("keyword") String keyword);
    // 推荐商品
    @Select("SELECT * FROM goods WHERE status = 0 AND category IN (${categories}) ORDER BY price ASC LIMIT #{limit}")
    List<Goods> recommendByCategories(@Param("categories") String categories, @Param("limit") int limit);
    // 原子增加浏览量，避免读-改-写竞态
    @org.apache.ibatis.annotations.Update("UPDATE goods SET browse_count = browse_count + 1 WHERE goods_id = #{goodsId}")
    int incrBrowseCount(@Param("goodsId") Long goodsId);

    @Select("SELECT COUNT(*) FROM goods WHERE status = 0")
    Long countOnSaleGoods();
    // 获取今日商品数
    @Select("SELECT COUNT(*) FROM goods WHERE DATE(create_time) = CURDATE()")
    Long countTodayGoods();
    // 获取用户商品列表
    @Select("SELECT * FROM goods WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Goods> selectByUserId(@Param("userId") Long userId);

    // 获取用户购买的物品
    @Select("SELECT * FROM goods WHERE buyer_id = #{userId} ORDER BY create_time DESC")
    List<Goods> selectBoughtByUserId(@Param("userId") Long userId);
}
