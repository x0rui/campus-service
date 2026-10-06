package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.ResourceDemand;
import com.campus.service.service.ResourceDemandService;
import com.campus.service.service.ResourceService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

// 求资源（需求匹配主线的学习侧落点）
@RestController
@RequestMapping("/api/resource-demand")
public class ResourceDemandController {

    private final ResourceDemandService demandService;
    private final ResourceService resourceService;

    public ResourceDemandController(ResourceDemandService demandService, ResourceService resourceService) {
        this.demandService = demandService;
        this.resourceService = resourceService;
    }

    // 1. 发布求资源 POST /publish
    @OpLog("发布求资源需求")
    @PostMapping("/publish")
    public Result<?> publish(HttpServletRequest request, @Valid @RequestBody ResourceDemand demand) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        demand.setUserId(userId);
        String text = demand.getTitle() + " " + (demand.getDescription() != null ? demand.getDescription() : "");
        if (!resourceService.checkSensitiveWords(text).isEmpty()) {
            return Result.fail("内容包含违规信息，请修改");
        }
        return Result.ok(demandService.publish(demand));
    }

    // 2. 求资源广场 GET /list
    @GetMapping("/list")
    public Result<List<ResourceDemand>> list(@RequestParam(defaultValue = "0") int page) {
        return Result.ok(demandService.list(page));
    }

    // 3. 匹配结果 GET /match/{id}（匹配到的资料 + 能帮助的同学）
    @GetMapping("/match/{demandId}")
    public Result<Map<String, Object>> match(@PathVariable Long demandId) {
        Map<String, Object> res = demandService.match(demandId);
        return res.isEmpty() ? Result.fail("需求不存在") : Result.ok(res);
    }

    // 4. 我发布的 GET /my
    @GetMapping("/my")
    public Result<List<ResourceDemand>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(demandService.getMy(userId));
    }

    // 5. 改状态 PUT /status/{id}  body: {status:1已解决|2已关闭}
    @PutMapping("/status/{demandId}")
    public Result<?> updateStatus(HttpServletRequest request, @PathVariable Long demandId,
                                  @RequestBody Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Object s = body.get("status");
        if (s == null) return Result.fail("缺少状态");
        Integer status = Integer.valueOf(s.toString());
        if (status != 1 && status != 2) return Result.fail("状态只能是1已解决或2已关闭");
        return demandService.updateStatus(demandId, userId, status) ? Result.ok() : Result.fail("无权操作");
    }
}
