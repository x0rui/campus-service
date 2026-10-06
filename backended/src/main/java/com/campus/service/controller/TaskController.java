package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Task;
import com.campus.service.service.SensitiveWordService;
import com.campus.service.service.TaskService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskService taskService;
    private final SensitiveWordService sensitiveWordService;

    public TaskController(TaskService taskService, SensitiveWordService sensitiveWordService) {
        this.taskService = taskService;
        this.sensitiveWordService = sensitiveWordService;
    }

    @PostMapping("/publish")
    public Result<?> publish(HttpServletRequest request, @Valid @RequestBody Task task) {
        Long userId = (Long) request.getAttribute("userId");
        task.setPublisherId(userId);

        String checkText = (task.getPickupLocation() != null ? task.getPickupLocation() : "")
                + " " + (task.getDeliveryLocation() != null ? task.getDeliveryLocation() : "")
                + " " + (task.getRemark() != null ? task.getRemark() : "");
        List<String> hits = sensitiveWordService.check(checkText);
        if (!hits.isEmpty()) {
            return Result.fail("发布内容包含违规信息，请修改");
        }

        if (task.getFee() != null && task.getFee().doubleValue() > 100) {
            return Result.fail("跑腿费不能超过100元");
        }
        if (task.getPickupLocation() == null || task.getDeliveryLocation() == null) {
            return Result.fail("请填写完整信息");
        }
        taskService.publish(task);
        return Result.ok(task);
    }

    @GetMapping("/square")
    public Result<List<Task>> square(@RequestParam(defaultValue = "0") int page) {
        return Result.ok(taskService.getSquareList(page));
    }

    @GetMapping("/type/{taskType}")
    public Result<List<Task>> byType(@PathVariable String taskType) {
        return Result.ok(taskService.getByType(taskType));
    }

    // 附近优先：传当前经纬度，按到取件点的距离升序
    @GetMapping("/nearby")
    public Result<List<Task>> nearby(@RequestParam(required = false) Double lat,
                                     @RequestParam(required = false) Double lng,
                                     @RequestParam(required = false) String type,
                                     @RequestParam(defaultValue = "0") int page) {
        return Result.ok(taskService.getNearbyList(lat, lng, type, page));
    }

    // 经纬度 → 文字地址（地图选点后回填）
    @GetMapping("/geocode")
    public Result<String> geocode(@RequestParam double lat, @RequestParam double lng) {
        return Result.ok(taskService.reverseGeocode(lat, lng));
    }

    // 接单者上报实时位置（存 Redis + WebSocket 推给发布者）
    @PostMapping("/location/report")
    public Result<?> reportLocation(HttpServletRequest request, @RequestBody java.util.Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Object taskId = body.get("taskId");
        Object lat = body.get("lat");
        Object lng = body.get("lng");
        if (taskId == null || lat == null || lng == null) return Result.fail("缺少参数");
        String err = taskService.reportLocation(Long.valueOf(taskId.toString()), userId,
                Double.parseDouble(lat.toString()), Double.parseDouble(lng.toString()));
        return err == null ? Result.ok() : Result.fail(err);
    }

    // 取最新位置（轮询兜底）
    @GetMapping("/location/{taskId}")
    public Result<String> getLocation(@PathVariable Long taskId) {
        return Result.ok(taskService.getLocation(taskId));
    }

    @GetMapping("/search")
    public Result<List<Task>> search(@RequestParam String keyword) {
        return Result.ok(taskService.search(keyword));
    }

    @GetMapping("/detail/{taskId}")
    public Result<Task> detail(HttpServletRequest request, @PathVariable Long taskId) {
        Long userId = (Long) request.getAttribute("userId");
        Task task = taskService.getDetail(taskId, userId);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        return Result.ok(task);
    }

    // 接单
    @OpLog("接单")
    @PostMapping("/take/{taskId}")
    public Result<?> takeTask(HttpServletRequest request, @PathVariable Long taskId) {
        Long userId = (Long) request.getAttribute("userId");
        String error = taskService.takeTask(taskId, userId);
        if (error != null) {
            return Result.fail(error);
        }
        return Result.ok("接单成功");
    }

    @PutMapping("/{taskId}")
    public Result<?> update(HttpServletRequest request, @PathVariable Long taskId, @RequestBody Task task) {
        Long userId = (Long) request.getAttribute("userId");
        boolean ok = taskService.updateTask(taskId, userId, task);
        return ok ? Result.ok("修改成功") : Result.fail("操作失败，仅可修改未被接单的任务");
    }

    // 取消任务（仅待接单时可取消，已接单后不能取消）
    @OpLog("取消任务")
    @PostMapping("/cancel/{taskId}")
    public Result<?> cancel(HttpServletRequest request, @PathVariable Long taskId) {
        Long userId = (Long) request.getAttribute("userId");
        boolean ok = taskService.cancelTask(taskId, userId);
        return ok ? Result.ok("已取消") : Result.fail("操作失败，已接单的任务不能取消");
    }

    // 接单者放弃任务，退回待接单
    @OpLog("放弃任务")
    @PostMapping("/giveup/{taskId}")
    public Result<?> giveUp(HttpServletRequest request, @PathVariable Long taskId) {
        Long userId = (Long) request.getAttribute("userId");
        String err = taskService.giveUpTask(taskId, userId);
        return err == null ? Result.ok("已放弃") : Result.fail(err);
    }

    // 完成任务（发布者或接单者均可确认完成）
    @OpLog("完成任务")
    @PostMapping("/complete/{taskId}")
    public Result<?> complete(HttpServletRequest request, @PathVariable Long taskId, @RequestBody java.util.Map<String, Object> body) {
        Long userId = (Long) request.getAttribute("userId");
        Long takerId = body.get("takerId") != null ? Long.valueOf(body.get("takerId").toString()) : null;
        boolean ok = taskService.completeTask(taskId, userId, takerId);
        return ok ? Result.ok("已完成") : Result.fail("操作失败");
    }

    @GetMapping("/my-published")
    public Result<List<Task>> myPublished(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(taskService.getPublishedTasks(userId));
    }

    @GetMapping("/my-taken")
    public Result<List<Task>> myTaken(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(taskService.getTakenTasks(userId));
    }
}
