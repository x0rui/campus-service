package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.entity.Resource;
import com.campus.service.entity.ResourceDemand;
import com.campus.service.entity.User;
import com.campus.service.mapper.ResourceDemandMapper;
import com.campus.service.mapper.ResourceMapper;
import com.campus.service.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 求资源：需求发布 → 按课程+关键词匹配资料 / 匹配上传过该课程的同学
@Service
public class ResourceDemandService {

    private final ResourceDemandMapper demandMapper;
    private final ResourceMapper resourceMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public ResourceDemandService(ResourceDemandMapper demandMapper, ResourceMapper resourceMapper,
                                 UserMapper userMapper, NotificationService notificationService) {
        this.demandMapper = demandMapper;
        this.resourceMapper = resourceMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    // 发布求资源：算一次匹配数，并把需求推给上传过该课程的同学
    @Transactional
    public ResourceDemand publish(ResourceDemand d) {
        d.setStatus(0);
        d.setMatchCount(countMatch(d.getCourse(), d.getKeyword()));
        demandMapper.insert(d);
        if (ResourceService.notBlank(d.getCourse())) {
            for (Long uid : resourceMapper.selectUserIdsByCourse(d.getCourse())) {
                if (uid.equals(d.getUserId())) continue;
                notificationService.send(uid, 6, "有人求资源",
                        "有同学在找《" + d.getCourse() + "》的资料：" + d.getTitle(), d.getDemandId());
            }
        }
        return d;
    }

    // 匹配已通过的资料：课程优先，关键词模糊
    public List<Resource> matchResources(String course, String keyword, int limit) {
        QueryWrapper<Resource> w = new QueryWrapper<>();
        w.eq("status", 1);
        if (ResourceService.notBlank(course)) w.eq("course", course);
        if (ResourceService.notBlank(keyword)) w.and(q -> q.like("title", keyword).or().like("description", keyword));
        w.orderByDesc("download_count").last("LIMIT " + limit);
        return resourceMapper.selectList(w);
    }

    private int countMatch(String course, String keyword) {
        QueryWrapper<Resource> w = new QueryWrapper<>();
        w.eq("status", 1);
        if (ResourceService.notBlank(course)) w.eq("course", course);
        if (ResourceService.notBlank(keyword)) w.and(q -> q.like("title", keyword).or().like("description", keyword));
        return resourceMapper.selectCount(w).intValue();
    }

    // 匹配结果：资料 + 能提供帮助的同学
    public Map<String, Object> match(Long demandId) {
        Map<String, Object> res = new HashMap<>();
        ResourceDemand d = demandMapper.selectById(demandId);
        if (d == null) return res;
        res.put("demand", d);
        res.put("resources", matchResources(d.getCourse(), d.getKeyword(), 20));
        res.put("users", matchUsers(d.getCourse(), d.getUserId()));
        return res;
    }

    // 上传过该课程的同学（只回必要的公开字段，不带 openid）
    private List<Map<String, Object>> matchUsers(String course, Long selfId) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (!ResourceService.notBlank(course)) return out;
        for (Long uid : resourceMapper.selectUserIdsByCourse(course)) {
            if (uid.equals(selfId)) continue;
            User u = userMapper.selectById(uid);
            if (u == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("userId", u.getUserId());
            m.put("nickName", u.getNickName());
            m.put("avatarUrl", u.getAvatarUrl());
            m.put("college", u.getCollege());
            m.put("major", u.getMajor());
            out.add(m);
        }
        return out;
    }

    public List<ResourceDemand> list(int page) {
        return demandMapper.selectList(new QueryWrapper<ResourceDemand>()
                .eq("status", 0).orderByDesc("create_time").last("LIMIT " + (page * 10) + ", 10"));
    }

    public List<ResourceDemand> getMy(Long userId) {
        return demandMapper.selectList(new QueryWrapper<ResourceDemand>()
                .eq("user_id", userId).orderByDesc("create_time"));
    }

    // 只有本人能改状态：1已解决 2已关闭
    public boolean updateStatus(Long demandId, Long userId, Integer status) {
        ResourceDemand d = demandMapper.selectById(demandId);
        if (d == null || !d.getUserId().equals(userId)) return false;
        d.setStatus(status);
        return demandMapper.updateById(d) > 0;
    }
}
