package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Resource;
import com.campus.service.service.ResourceService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

// 学习资源共享模块：资料上传/检索/下载/收藏/审核
@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    // 1. 上传资料 POST /publish（入库待审核）
    @OpLog("上传学习资料")
    @PostMapping("/publish")
    public Result<?> publish(HttpServletRequest request, @Valid @RequestBody Resource resource) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        resource.setUserId(userId);
        String text = resource.getTitle() + " " + (resource.getDescription() != null ? resource.getDescription() : "");
        if (!resourceService.checkSensitiveWords(text).isEmpty()) {
            return Result.fail("发布内容包含违规信息，请修改");
        }
        resourceService.publish(resource);
        return Result.ok(resource);
    }

    // 2. 资料列表 GET /list（课程/类型/关键词三条件可选）
    @GetMapping("/list")
    public Result<List<Resource>> list(@RequestParam(required = false) String course,
                                       @RequestParam(required = false) String type,
                                       @RequestParam(required = false) String keyword,
                                       @RequestParam(defaultValue = "0") int page) {
        return Result.ok(resourceService.list(course, type, keyword, page));
    }

    // 3. 课程列表 GET /courses（筛选下拉用）
    @GetMapping("/courses")
    public Result<List<String>> courses() {
        return Result.ok(resourceService.getCourses());
    }

    // 4. 资料详情 GET /detail/{id}
    @GetMapping("/detail/{resourceId}")
    public Result<Resource> detail(@PathVariable Long resourceId) {
        Resource r = resourceService.getDetail(resourceId);
        return r == null ? Result.fail("资料不存在") : Result.ok(r);
    }

    // 5. 是否已收藏 GET /is-collect/{id}
    @GetMapping("/is-collect/{resourceId}")
    public Result<Boolean> isCollect(HttpServletRequest request, @PathVariable Long resourceId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(resourceService.isFavorite(resourceId, userId));
    }

    // 6. 我上传的 GET /my（含待审核/被拒）
    @GetMapping("/my")
    public Result<List<Resource>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(resourceService.getMy(userId));
    }

    // 7. 下载 POST /download/{id}（计数+留痕，返回带 fileUrl 的资料）
    @OpLog("下载学习资料")
    @PostMapping("/download/{resourceId}")
    public Result<Resource> download(HttpServletRequest request, @PathVariable Long resourceId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Resource r = resourceService.download(resourceId, userId);
        return r == null ? Result.fail("资料不存在或未通过审核") : Result.ok(r);
    }

    // 8. 收藏/取消 POST /collect/{id}
    @PostMapping("/collect/{resourceId}")
    public Result<Boolean> collect(HttpServletRequest request, @PathVariable Long resourceId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(resourceService.toggleCollect(resourceId, userId));
    }

    // 9. 我的收藏 GET /my-collect
    @GetMapping("/my-collect")
    public Result<List<Resource>> myCollect(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(resourceService.getMyFavorite(userId));
    }

    // 10. 删除自己的资料 DELETE /{id}
    @DeleteMapping("/{resourceId}")
    public Result<?> remove(HttpServletRequest request, @PathVariable Long resourceId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return resourceService.remove(resourceId, userId) ? Result.ok() : Result.fail("只能删除自己上传的资料");
    }

    // 11. 管理员审核 PUT /audit/{id}
    @OpLog("审核学习资料")
    @PutMapping("/audit/{resourceId}")
    public Result<?> audit(HttpServletRequest request, @PathVariable Long resourceId,
                           @RequestBody Map<String, Object> body) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(403, "无权限");
        Object statusObj = body.get("status");
        if (statusObj == null) return Result.fail("缺少审核状态");
        Integer status = Integer.valueOf(statusObj.toString());
        if (status != 1 && status != 2) return Result.fail("审核状态只能是1通过或2拒绝");
        String reason = body.get("rejectReason") == null ? "" : body.get("rejectReason").toString();
        return resourceService.audit(resourceId, status, reason) ? Result.ok() : Result.fail("资料不存在");
    }

    // 12. 后台列表 GET /admin/list?status=&page=
    @GetMapping("/admin/list")
    public Result<List<Resource>> adminList(HttpServletRequest request,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "0") int page) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(403, "无权限");
        return Result.ok(resourceService.adminList(status, page));
    }
}
