package com.campus.service.controller;

import com.campus.service.dto.Result;
import com.campus.service.entity.Notification;
import com.campus.service.service.NotificationService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public Result<List<Notification>> list(HttpServletRequest request,
                                           @RequestParam(defaultValue = "0") int page) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(notificationService.getMyList(userId, page));
    }

    @PostMapping("/read/{id}")
    public Result<?> read(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        notificationService.markAsRead(id, userId);
        return Result.ok();
    }

    @PostMapping("/read-all")
    public Result<?> readAll(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        notificationService.markAllAsRead(userId);
        return Result.ok();
    }
}
