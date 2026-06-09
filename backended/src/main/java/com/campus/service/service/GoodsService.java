package com.campus.service.service;

import com.campus.service.entity.Goods;
import com.campus.service.mapper.GoodsMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class GoodsService {

    private final GoodsMapper goodsMapper;
    private final SensitiveWordService sensitiveWordService;

    public GoodsService(GoodsMapper goodsMapper, SensitiveWordService sensitiveWordService) {
        this.goodsMapper = goodsMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    @CacheEvict(cacheNames = "goods:index", allEntries = true)
    public Goods publish(Goods goods) {
        goods.setStatus(0);
        goods.setBrowseCount(0);
        goodsMapper.insert(goods);
        return goods;
    }

    public List<String> checkSensitiveWords(String text) {
        return sensitiveWordService.check(text);
    }

    @Cacheable(cacheNames = "goods:index", key = "#page")
    public List<Goods> getIndexList(int page) {
        return goodsMapper.selectOnSaleList(page * 10, 10);
    }

    public List<Goods> getOnSaleList() {
        return goodsMapper.selectAllOnSale();
    }

    public List<Goods> getAllGoods() {
        return goodsMapper.selectList(null);
    }

    public List<Goods> getByCategory(String category) {
        return goodsMapper.selectByCategory(category);
    }

    public List<Goods> search(String keyword) {
        return goodsMapper.searchByKeyword(keyword);
    }

    // 获取详情（原子增加浏览量，避免竞态）
    public Goods getDetail(Long goodsId, Long userId) {
        return getDetail(goodsId, userId, false);
    }

    public Goods getDetail(Long goodsId, Long userId, boolean skipView) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods != null && !skipView) {
            goodsMapper.incrBrowseCount(goodsId);
        }
        return goods;
    }

    public List<Goods> getUserGoods(Long userId) {
        return goodsMapper.selectByUserId(userId);
    }

    public List<Goods> getBoughtGoods(Long userId) {
        return goodsMapper.selectBoughtByUserId(userId);
    }

    // 状态流转校验：0→1(售出) 或 0→2(下架)，其他不允许
    @CacheEvict(cacheNames = "goods:index", allEntries = true)
    public String updateStatus(Long goodsId, Integer status, Long buyerId, Long sellerId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return "物品不存在";
        if (!goods.getUserId().equals(sellerId)) return "无权操作";
        if (goods.getStatus() != 0) return "仅可操作在售物品";
        // 只允许 1(售出) 和 2(下架)
        if (status != 1 && status != 2) return "无效状态";
        // 售出必须指定买家
        if (status == 1) {
            if (buyerId == null) return "请指定买家ID";
            if (buyerId.equals(sellerId)) return "不能卖给自己";
        }
        goods.setStatus(status);
        if (buyerId != null) goods.setBuyerId(buyerId);
        goodsMapper.updateById(goods);
        return null;
    }

    @CacheEvict(cacheNames = "goods:index", allEntries = true)
    public boolean updateGoods(Long goodsId, Long userId, Goods update) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null || !goods.getUserId().equals(userId) || goods.getStatus() != 0) {
            return false;
        }
        if (update.getTitle() != null) goods.setTitle(update.getTitle());
        if (update.getDescription() != null) goods.setDescription(update.getDescription());
        if (update.getCategory() != null) goods.setCategory(update.getCategory());
        if (update.getPrice() != null) goods.setPrice(update.getPrice());
        if (update.getImages() != null) goods.setImages(update.getImages());
        return goodsMapper.updateById(goods) > 0;
    }

    // 管理员强制操作，不受状态流转限制
    @CacheEvict(cacheNames = "goods:index", allEntries = true)
    public boolean adminForceOffline(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) return false;
        goods.setStatus(2);
        return goodsMapper.updateById(goods) > 0;
    }

    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("onSaleGoods", goodsMapper.countOnSaleGoods());
        stats.put("todayGoods", goodsMapper.countTodayGoods());
        return stats;
    }
}
