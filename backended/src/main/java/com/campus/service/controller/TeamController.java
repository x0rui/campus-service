package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Team;
import com.campus.service.entity.TeamJoin;
import com.campus.service.service.TeamService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    // 创建组局
    @OpLog("创建组局")
    @PostMapping("/create")
    public Result<Team> create(HttpServletRequest request, @RequestBody Team team) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        team.setUserId(userId);
        return Result.ok(teamService.create(team));
    }

    // 列表
    @GetMapping("/list")
    public Result<List<Team>> list(@RequestParam(defaultValue = "0") int page) {
        return Result.ok(teamService.getActiveList(page));
    }

    // 按标签筛选
    @GetMapping("/tag/{tag}")
    public Result<List<Team>> byTag(@PathVariable String tag) {
        return Result.ok(teamService.getByTag(tag));
    }

    // 搜索
    @GetMapping("/search")
    public Result<List<Team>> search(@RequestParam String keyword) {
        return Result.ok(teamService.search(keyword));
    }

    // 详情
    @GetMapping("/detail/{teamId}")
    public Result<Team> detail(@PathVariable Long teamId) {
        return Result.ok(teamService.getDetail(teamId));
    }

    // 申请加入
    @OpLog("申请加入组局")
    @PostMapping("/apply/{teamId}")
    public Result<?> apply(HttpServletRequest request, @PathVariable Long teamId) {
        Long userId = (Long) request.getAttribute("userId");
        String error = teamService.applyJoin(teamId, userId);
        if (error != null) return Result.fail(error);
        return Result.ok("申请成功，等待发起人审核");
    }

    // 审核申请
    @OpLog("审核组局申请")
    @PutMapping("/approve/{joinId}")
    public Result<?> approve(HttpServletRequest request, @PathVariable Long joinId, @RequestBody Map<String, Boolean> body) {
        Long userId = (Long) request.getAttribute("userId");
        boolean approved = body.getOrDefault("approved", false);
        String error = teamService.approveJoin(joinId, userId, approved);
        if (error != null) return Result.fail(error);
        return Result.ok(approved ? "已通过" : "已拒绝");
    }

    // 待审核申请列表
    @GetMapping("/pending/{teamId}")
    public Result<List<TeamJoin>> pending(HttpServletRequest request, @PathVariable Long teamId) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(teamService.getPendingJoins(teamId, userId));
    }

    // 生成签到码
    @PostMapping("/sign-code/{teamId}")
    public Result<String> generateSignCode(HttpServletRequest request, @PathVariable Long teamId) {
        Long userId = (Long) request.getAttribute("userId");
        String code = teamService.generateSignCode(teamId, userId);
        if (code == null) return Result.fail("无权操作");
        return Result.ok(code);
    }

    // 签到
    @PostMapping("/checkin/{teamId}")
    public Result<?> checkIn(HttpServletRequest request, @PathVariable Long teamId, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String code = body.get("code");
        String error = teamService.checkIn(teamId, code, userId);
        if (error != null) return Result.fail(error);
        return Result.ok("签到成功");
    }

    // 我的组局
    @GetMapping("/my")
    public Result<List<Team>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(teamService.getMyTeams(userId));
    }

    // 我加入的
    @GetMapping("/joined")
    public Result<List<Team>> joined(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(teamService.getJoinedTeams(userId));
    }

    // 取消组局
    @OpLog("取消组局")
    @PostMapping("/cancel/{teamId}")
    public Result<?> cancel(HttpServletRequest request, @PathVariable Long teamId) {
        Long userId = (Long) request.getAttribute("userId");
        boolean ok = teamService.cancel(teamId, userId);
        return ok ? Result.ok("已取消") : Result.fail("操作失败");
    }

    // 编辑组局
    @OpLog("编辑组局")
    @PutMapping("/update/{teamId}")
    public Result<Team> update(HttpServletRequest request, @PathVariable Long teamId, @RequestBody Team team) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Team updated = teamService.update(teamId, userId, team);
        if (updated == null) return Result.fail("无权操作或组局不存在");
        return Result.ok(updated);
    }

    // 踢出成员
    @OpLog("踢出成员")
    @PostMapping("/kick/{teamId}/{userId}")
    public Result<?> kick(HttpServletRequest request, @PathVariable Long teamId, @PathVariable Long userId) {
        Long creatorId = (Long) request.getAttribute("userId");
        String error = teamService.kickMember(teamId, creatorId, userId);
        return error != null ? Result.fail(error) : Result.ok("已踢出");
    }

    // 成员列表
    @GetMapping("/members/{teamId}")
    public Result<List<TeamJoin>> members(@PathVariable Long teamId) {
        return Result.ok(teamService.getMembers(teamId));
    }
}
