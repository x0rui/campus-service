package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Goods;
import com.campus.service.entity.OperationLog;
import com.campus.service.entity.Task;
import com.campus.service.service.AdminService;
import com.campus.service.service.GoodsService;
import com.campus.service.service.TaskService;
import com.campus.service.service.UserService;
import com.campus.service.mapper.OperationLogMapper;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final GoodsService goodsService;
    private final TaskService taskService;
    private final OperationLogMapper operationLogMapper;

    public AdminController(AdminService adminService, UserService userService,
                           GoodsService goodsService, TaskService taskService,
                           OperationLogMapper operationLogMapper) {
        this.adminService = adminService;
        this.userService = userService;
        this.goodsService = goodsService;
        this.taskService = taskService;
        this.operationLogMapper = operationLogMapper;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        return role != null && role == 2;
    }

    // 获取仪表盘数据
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        return Result.ok(adminService.getDashboardStats());
    }

    // 获取所有物品
    @GetMapping("/goods")
    public Result<List<Goods>> allGoods(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        return Result.ok(goodsService.getAllGoods());
    }

    // 获取所有任务
    @GetMapping("/tasks")
    public Result<List<Task>> allTasks(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        return Result.ok(taskService.getAllTasks());
    }

    // 获取所有操作日志（支持关键词搜索 + 类型筛选）
    @GetMapping("/logs")
    public Result<List<OperationLog>> allLogs(HttpServletRequest request,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String type) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.OperationLog> qw =
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            qw.like(com.campus.service.entity.OperationLog::getOperation, keyword.trim());
        }
        if (type != null && !type.isEmpty()) {
            if ("goods".equals(type)) qw.like(com.campus.service.entity.OperationLog::getOperation, "闲置");
            else if ("task".equals(type)) qw.like(com.campus.service.entity.OperationLog::getOperation, "任务");
            else if ("team".equals(type)) qw.like(com.campus.service.entity.OperationLog::getOperation, "组局");
            else if ("post".equals(type)) qw.and(w -> w.like(com.campus.service.entity.OperationLog::getOperation, "帖子").or().like(com.campus.service.entity.OperationLog::getOperation, "公告").or().like(com.campus.service.entity.OperationLog::getOperation, "评论").or().like(com.campus.service.entity.OperationLog::getOperation, "置顶").or().like(com.campus.service.entity.OperationLog::getOperation, "删除"));
            else if ("club".equals(type)) qw.and(w -> w.like(com.campus.service.entity.OperationLog::getOperation, "社团").or().like(com.campus.service.entity.OperationLog::getOperation, "入驻"));
            else if ("report".equals(type)) qw.like(com.campus.service.entity.OperationLog::getOperation, "举报");
        }
        qw.orderByDesc(com.campus.service.entity.OperationLog::getCreateTime);
        return Result.ok(operationLogMapper.selectList(qw));
    }

    // 获取所有用户
    @GetMapping("/users")
    public Result<List<com.campus.service.entity.User>> userList(HttpServletRequest request,
                                                                  @RequestParam(required = false) String userId) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        return Result.ok(adminService.getAllUsers(userId));
    }

    @OpLog("修改用户角色")
    @PutMapping("/user-role/{userId}")
    public Result<?> updateUserRole(HttpServletRequest request, @PathVariable Long userId,
                                    @RequestBody Map<String, Integer> body) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        Long adminId = (Long) request.getAttribute("userId");
        if (adminId.equals(userId)) return Result.fail("不能修改自己的角色");
        Integer role = body.get("role");
        if (role == null || role < 0 || role > 2) return Result.fail("无效的角色");
        userService.updateRole(userId, role);
        return Result.ok();
    }

    @OpLog("修改用户状态")
    @PutMapping("/user-status/{userId}")
    public Result<?> updateUserStatus(HttpServletRequest request, @PathVariable Long userId,
                                      @RequestBody Map<String, Integer> body) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        Long adminId = (Long) request.getAttribute("userId");
        if (adminId.equals(userId)) return Result.fail("不能禁用自己");
        Integer status = body.get("status");
        if (status == null) return Result.fail("无效的状态");
        com.campus.service.entity.User user = userService.getUserById(userId);
        if (user == null) return Result.fail("用户不存在");
        if (user.getRole() == 2) return Result.fail("不能禁用系统管理员");
        user.setStatus(status);
        userService.updateUser(user);
        return Result.ok();
    }

    @OpLog("管理员强制下架物品")
    @PutMapping("/goods/offline/{goodsId}")
    public Result<?> offlineGoods(HttpServletRequest request, @PathVariable Long goodsId) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        goodsService.adminForceOffline(goodsId);
        return Result.ok("已下架");
    }

    @OpLog("管理员强制取消任务")
    @PutMapping("/task/offline/{taskId}")
    public Result<?> offlineTask(HttpServletRequest request, @PathVariable Long taskId) {
        if (!isAdmin(request)) return Result.fail(403, "无权限");
        boolean ok = taskService.adminForceCancel(taskId);
        return ok ? Result.ok("已取消") : Result.fail("操作失败，已完成或已取消的任务无需处理");
    }
}
