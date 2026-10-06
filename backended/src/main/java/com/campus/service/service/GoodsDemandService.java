package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.entity.Goods;
import com.campus.service.entity.GoodsDemand;
import com.campus.service.mapper.GoodsDemandMapper;
import com.campus.service.mapper.GoodsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 求购：与「求资源」「校园圈求助帖」共用同一套"需求发布 → 匹配供给方"的设计
@Service
public class GoodsDemandService {

    private final GoodsDemandMapper demandMapper;
    private final GoodsMapper goodsMapper;
    private final NotificationService notificationService;

    public GoodsDemandService(GoodsDemandMapper demandMapper, GoodsMapper goodsMapper,
                              NotificationService notificationService) {
        this.demandMapper = demandMapper;
        this.goodsMapper = goodsMapper;
        this.notificationService = notificationService;
    }

    // 发布求购：算一次匹配数，并把需求推给匹配到的卖家
    @Transactional
    public GoodsDemand publish(GoodsDemand d) {
        d.setStatus(0);
        List<Goods> matched = matchList(d, 10);
        d.setMatchCount((int) countMatch(d));
        demandMapper.insert(d);

        for (Goods g : matched) {
            if (g.getUserId() == null || g.getUserId().equals(d.getUserId())) continue;
            notificationService.send(g.getUserId(), 4, "有人求购",
                    "有同学在求购「" + d.getTitle() + "」，你发布的物品可能符合", d.getDemandId());
        }
        return d;
    }

    // 匹配条件：在售 + 分类一致 + 标题含关键词 + 不超过可接受最高价
    private QueryWrapper<Goods> buildMatch(GoodsDemand d) {
        QueryWrapper<Goods> w = new QueryWrapper<>();
        w.eq("status", 0);
        if (ResourceService.notBlank(d.getCategory())) w.eq("category", d.getCategory());
        if (ResourceService.notBlank(d.getKeyword())) w.like("title", d.getKeyword());
        if (d.getMaxPrice() != null) w.le("price", d.getMaxPrice());
        return w;
    }

    private List<Goods> matchList(GoodsDemand d, int limit) {
        QueryWrapper<Goods> w = buildMatch(d);
        w.orderByAsc("price").last("LIMIT " + limit);
        return goodsMapper.selectList(w);
    }

    private long countMatch(GoodsDemand d) {
        return goodsMapper.selectCount(buildMatch(d));
    }

    // 匹配结果：一条求购对应的所有在售物品
    public List<Goods> matchGoods(Long demandId, int limit) {
        GoodsDemand d = demandMapper.selectById(demandId);
        if (d == null) return new ArrayList<>();
        return matchList(d, limit);
    }

    public List<GoodsDemand> list(int page) {
        return demandMapper.selectList(new QueryWrapper<GoodsDemand>()
                .eq("status", 0).orderByDesc("create_time").last("LIMIT " + (page * 10) + ", 10"));
    }

    public List<GoodsDemand> getMy(Long userId) {
        return demandMapper.selectList(new QueryWrapper<GoodsDemand>()
                .eq("user_id", userId).orderByDesc("create_time"));
    }

    // 只有本人能改状态：1已找到 2已关闭
    public boolean updateStatus(Long demandId, Long userId, Integer status) {
        GoodsDemand d = demandMapper.selectById(demandId);
        if (d == null || !d.getUserId().equals(userId)) return false;
        d.setStatus(status);
        return demandMapper.updateById(d) > 0;
    }
}
