package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Report;
import com.campus.service.service.ReportService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @OpLog("提交举报")
    @PostMapping("/create")
    public Result<Report> create(HttpServletRequest request, @RequestBody Report r) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        if (r.getTargetType() == null || r.getTargetId() == null)
            return Result.fail("请指定举报对象");
        if (r.getReason() == null || r.getReason().trim().isEmpty())
            return Result.fail("请选择举报原因");
        if (r.getDescription() == null || r.getDescription().trim().isEmpty())
            return Result.fail("请填写举报描述");
        r.setReporterId(userId);
        return Result.ok(reportService.create(r));
    }

    @GetMapping("/my")
    public Result<List<Report>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(reportService.getMy(userId));
    }

    @GetMapping("/admin/list")
    public Result<List<Report>> adminList(
            @RequestParam(defaultValue = "all") String targetType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(401, "无权访问");
        return Result.ok(reportService.getAdminList(targetType, status, keyword));
    }

    @OpLog("处理举报")
    @PostMapping("/admin/handle/{id}")
    public Result<?> handle(HttpServletRequest request, @PathVariable Long id,
                             @RequestBody Map<String, Object> body) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(401, "无权访问");
        Long handlerId = (Long) request.getAttribute("userId");
        int status = Integer.parseInt(body.getOrDefault("status", "0").toString());
        String handleNote = (String) body.getOrDefault("handleNote", "");
        boolean ok = reportService.handle(id, status, handlerId, handleNote);
        return ok ? Result.ok("处理成功") : Result.fail("处理失败，举报可能已被处理");
    }

    @GetMapping("/detail/{id}")
    public Result<Report> detail(@PathVariable Long id, HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(401, "无权访问");
        return Result.ok(reportService.getDetail(id));
    }
}
