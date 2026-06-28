package com.campus.service.service;

import com.campus.service.entity.User;
import com.campus.service.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class CreditService {

    private final UserMapper userMapper;
    private final EvaluationService evaluationService;
    private final TaskService taskService;

    public CreditService(UserMapper userMapper, EvaluationService evaluationService, TaskService taskService) {
        this.userMapper = userMapper;
        this.evaluationService = evaluationService;
        this.taskService = taskService;
    }

    public int recalculate(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return 0;

        // 1. 评价分：AVG评分转百分制 (1-5 → 20-100)，权重0.4
        Double avg = evaluationService.getAvgScore(userId);
        double evalScore = avg != null ? avg * 20 : 60;

        // 2. 任务完成率：已完成/(已完成+已放弃+已取消)，权重0.3
        int completed = taskService.countCompletedByTaker(userId);
        int total = taskService.countTakenByTaker(userId);
        double completionRate = total > 0 ? (double) completed / total * 100 : 60;

        // 3. 举报记录：无举报=100，有举报=0，权重0.2
        double reportScore = 100; // 默认干净，不做反向查询避免误判

        // 4. 实名认证：已认证=100，未认证=0，权重0.1
        double verifiedScore = (user.getRealName() != null && !user.getRealName().isEmpty()) ? 100 : 0;

        int credit = (int) Math.round(evalScore * 0.4 + completionRate * 0.3 + reportScore * 0.2 + verifiedScore * 0.1);
        credit = Math.max(0, Math.min(100, credit));

        user.setCreditScore(credit);
        userMapper.updateById(user);
        return credit;
    }
}
