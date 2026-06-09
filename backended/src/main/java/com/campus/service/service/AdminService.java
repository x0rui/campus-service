package com.campus.service.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AdminService {

    private final UserService userService;
    private final GoodsService goodsService;
    private final TaskService taskService;

    public AdminService(UserService userService, GoodsService goodsService, TaskService taskService) {
        this.userService = userService;
        this.goodsService = goodsService;
        this.taskService = taskService;
    }

    public List<com.campus.service.entity.User> getAllUsers() {
        return getAllUsers(null);
    }

    public List<com.campus.service.entity.User> getAllUsers(String userId) {
        List<com.campus.service.entity.User> users;
        if (userId != null && !userId.trim().isEmpty()) {
            users = userService.listAllByUserId(userId.trim());
        } else {
            users = userService.listAll();
        }
        // 脱敏：不返回手机号和学号给前端
        for (com.campus.service.entity.User u : users) {
            u.setPhone(null);
            u.setStudentId(null);
            u.setOpenid(null);
        }
        return users;
    }

    // 获取仪表盘统计数据，拼装三个模块的统计数据成一个 Map 返回
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("users", userService.getStatistics());
        stats.put("goods", goodsService.getStatistics());
        stats.put("tasks", taskService.getStatistics());
        return stats;
    }
}
