package com.campus.service.controller;

import com.campus.service.dto.Result;
import com.campus.service.entity.Evaluation;
import javax.validation.Valid;
import com.campus.service.service.EvaluationService;
import com.campus.service.service.UserService;
import com.campus.service.mapper.TeamJoinMapper;
import com.campus.service.mapper.TeamMapper;
import com.campus.service.entity.Team;
import com.campus.service.entity.TeamJoin;
import com.campus.service.entity.User;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/evaluation")
public class EvaluationController {

    private final EvaluationService evaluationService;
    private final UserService userService;
    private final TeamJoinMapper teamJoinMapper;
    private final TeamMapper teamMapper;

    public EvaluationController(EvaluationService evaluationService, UserService userService,
                                TeamJoinMapper teamJoinMapper, TeamMapper teamMapper) {
        this.evaluationService = evaluationService;
        this.userService = userService;
        this.teamJoinMapper = teamJoinMapper;
        this.teamMapper = teamMapper;
    }

    @PostMapping("/submit")
    public Result<Evaluation> submit(HttpServletRequest request, @Valid @RequestBody Evaluation evaluation) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        evaluation.setEvaluatorId(userId);

        // 检查实名认证
        User u = userService.getUserById(userId);
        if (u == null || u.getRealName() == null || u.getRealName().isEmpty()) {
            return Result.fail("请先完成实名认证");
        }

        if (evaluation.getScore() == null || evaluation.getScore() < 1 || evaluation.getScore() > 5) {
            return Result.fail("评分需在1-5之间");
        }

        // 不能评价自己
        if (userId.equals(evaluation.getTargetId())) {
            return Result.fail("不能评价自己");
        }

        // 组局评价：检查是否为成员，普通成员需先签到
        if (evaluation.getOrderType() != null && evaluation.getOrderType() == 2) {
            TeamJoin join = teamJoinMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TeamJoin>()
                    .eq(TeamJoin::getTeamId, evaluation.getOrderId())
                    .eq(TeamJoin::getUserId, userId)
                    .eq(TeamJoin::getStatus, 1));
            if (join == null) return Result.fail("你不是该组局成员");
            Team team = teamMapper.selectById(evaluation.getOrderId());
            boolean isCreator = team != null && team.getUserId().equals(userId);
            if (!isCreator && (join.getCheckedIn() == null || join.getCheckedIn() != 1)) {
                return Result.fail("请先签到后再评价");
            }
        }

        // 检查是否已评价过
        if (evaluationService.hasEvaluated(evaluation.getOrderId(), evaluation.getOrderType(), userId)) {
            return Result.fail("已评价过了，不可重复评价");
        }

        evaluationService.evaluate(evaluation);
        return Result.ok(evaluation);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Evaluation>> userEvaluations(@PathVariable Long userId) {
        List<Evaluation> list = evaluationService.getUserEvaluations(userId);
        // 匿名评价屏蔽评价者身份
        for (Evaluation e : list) {
            if (e.getIsAnonymous() != null && e.getIsAnonymous() == 1) {
                e.setEvaluatorId(null);
            }
        }
        return Result.ok(list);
    }

    @GetMapping("/given/{userId}")
    public Result<List<Evaluation>> givenEvaluations(@PathVariable Long userId) {
        return Result.ok(evaluationService.getGivenEvaluations(userId));
    }

    @GetMapping("/avg-score/{userId}")
    public Result<Double> avgScore(@PathVariable Long userId) {
        Double score = evaluationService.getAvgScore(userId);
        return Result.ok(score != null ? score : 0.0);
    }

    @GetMapping("/top-users")
    public Result<List<java.util.Map<String, Object>>> topUsers() {
        return Result.ok(evaluationService.getTopUsers());
    }

    @GetMapping("/top-teams")
    public Result<List<java.util.Map<String, Object>>> topTeams() {
        return Result.ok(evaluationService.getTopTeams());
    }

    @GetMapping("/top-clubs")
    public Result<List<java.util.Map<String, Object>>> topClubs() {
        return Result.ok(evaluationService.getTopClubs());
    }

    @GetMapping("/check")
    public Result<Boolean> checkEvaluated(HttpServletRequest request, @RequestParam Long orderId, @RequestParam Integer orderType) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(evaluationService.hasEvaluated(orderId, orderType, userId));
    }
}
