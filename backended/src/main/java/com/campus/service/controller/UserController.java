package com.campus.service.controller;

import com.campus.service.dto.Result;
import com.campus.service.entity.User;
import com.campus.service.service.UserService;
import com.campus.service.service.WechatService;
import com.campus.service.service.BadgeService;
import com.campus.service.util.JwtUtil;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final WechatService wechatService;
    private final JwtUtil jwtUtil;
    private final BadgeService badgeService;

    public UserController(UserService userService, WechatService wechatService, JwtUtil jwtUtil, BadgeService badgeService) {
        this.userService = userService;
        this.wechatService = wechatService;
        this.jwtUtil = jwtUtil;
        this.badgeService = badgeService;
    }

    /**
     * 整个登录流程（一句话概括）
     * 用户微信登录 → 后端用 code 换 openid → 生成 JWT 令牌 → 返回给前端 → 前端每次请求带上令牌 → 后端拦截器校验令牌 → 拿到用户身份
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String nickName = body.getOrDefault("nickName", "微信用户");
        String avatarUrl = body.getOrDefault("avatarUrl", "");
        if (code == null || code.isEmpty()) {
            return Result.fail("code不能为空");
        }
        // 后端用 code 换 openid（真机走微信API，开发工具模拟器dev模式走hash降级）
        String openid = wechatService.getOpenId(code);
        if (openid == null) {
            openid = "wx_" + Integer.toHexString(code.hashCode());
        }
        Map<String, Object> result = userService.login(openid, nickName, avatarUrl);
        if (result == null) {
            return Result.fail("账号已被禁用");
        }
        // 脱敏：不返回敏感信息到前端
        if (result.get("user") instanceof User) {
            User u = (User) result.get("user");
            u.setOpenid(null);
            u.setPhone(null);
            u.setStudentId(null);
        }
        return Result.ok(result);
    }

    // 获取用户信息
    @GetMapping("/info")
    public Result<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String token = (String) request.getAttribute("token");
        Integer tokenRole = (Integer) request.getAttribute("role");
        if (userId == null) return Result.fail(401, "请先登录");
        User user = userService.getUserById(userId);
        if (user == null) return Result.fail(401, "用户不存在");

        // 如果角色变了，自动刷新 token
        // 脱敏
        user.setOpenid(null);
        user.setPhone(null);
        user.setStudentId(null);
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("user", user);
        if (tokenRole != null && !tokenRole.equals(user.getRole()) && token != null) {
            String newToken = jwtUtil.generateToken(userId, user.getOpenid(), user.getRole());
            map.put("newToken", newToken);
        }
        return Result.ok(map);
    }

    // 绑定信息
    @PostMapping("/bind")
    public Result<?> bindInfo(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String realName = body.get("realName");
        String studentId = body.get("studentId");
        String phone = body.get("phone");
        String college = body.getOrDefault("college", "");
        String major = body.getOrDefault("major", "");
        String className = body.getOrDefault("className", "");
        Integer age = null;
        try { if (body.get("age") != null) age = Integer.parseInt(body.get("age")); } catch (NumberFormatException e) {}
        String gender = body.getOrDefault("gender", "");
        if (realName == null || studentId == null || phone == null) {
            return Result.fail("请填写完整信息");
        }
        boolean ok = userService.bindInfo(userId, realName, studentId, phone, college, major, className, age, gender);
        return ok ? Result.ok() : Result.fail("绑定失败");
    }

    // 更新用户信息
    @PutMapping("/info")
    public Result<?> updateInfo(HttpServletRequest request, @RequestBody User user) {
        Long userId = (Long) request.getAttribute("userId");
        user.setUserId(userId);
        boolean ok = userService.updateUser(user);
        return ok ? Result.ok() : Result.fail("更新失败");
    }

    // 获取用户信息
    @GetMapping("/simple/{userId}")
    public Result<User> getSimpleUser(@PathVariable Long userId) {
        User user = userService.getUserById(userId);
        if (user != null) {
            user.setOpenid(null);
            user.setPhone(null);
            user.setStudentId(null);
        }
        return Result.ok(user);
    }

    // 推荐搭子（根据兴趣爱好匹配）
    @GetMapping("/recommend")
    public Result<List<User>> recommend(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        List<User> users = userService.getRecommendedUsers(userId);
        for (User u : users) {
            u.setOpenid(null);
            u.setPhone(null);
            u.setStudentId(null);
        }
        return Result.ok(users);
    }

    // 未读消息数 + 待处理项目数
    @GetMapping("/badge")
    public Result<?> badge(HttpServletRequest request,
                           @RequestParam(required = false) Long reportsViewTime) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        if (userId == null) return Result.fail(401, "请先登录");
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();

        counts.put("unreadMessages", badgeService.getUnreadMessages(userId));
        counts.put("pendingClubApps", badgeService.getPendingClubApps(userId));
        counts.put("handledReports", badgeService.getHandledReports(userId, reportsViewTime));
        counts.put("pendingTeamJoins", badgeService.getPendingTeamJoins(userId));

        if (role != null && role >= 2) {
            counts.put("pendingClubChecks", badgeService.getPendingClubChecks());
            counts.put("pendingReports", badgeService.getPendingReports());
        }

        return Result.ok(counts);
    }
}
