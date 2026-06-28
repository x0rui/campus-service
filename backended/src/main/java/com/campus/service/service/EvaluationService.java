package com.campus.service.service;

import com.campus.service.entity.Evaluation;
import com.campus.service.mapper.EvaluationMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final CreditService creditService;

    public EvaluationService(EvaluationMapper evaluationMapper, @Lazy CreditService creditService) {
        this.evaluationMapper = evaluationMapper;
        this.creditService = creditService;
    }

    @CacheEvict(cacheNames = {"eval:topUsers", "eval:topTeams", "eval:topClubs"}, allEntries = true)
    public Evaluation evaluate(Evaluation evaluation) {
        evaluationMapper.insert(evaluation);
        try {
            creditService.recalculate(evaluation.getTargetId());
        } catch (Exception ignored) {}
        return evaluation;
    }

    public List<Evaluation> getUserEvaluations(Long userId) {
        return evaluationMapper.selectByTargetId(userId);
    }

    public List<Evaluation> getGivenEvaluations(Long userId) {
        return evaluationMapper.selectByEvaluatorId(userId);
    }

    @Cacheable(cacheNames = "eval:topUsers")
    public List<java.util.Map<String, Object>> getTopUsers() {
        return evaluationMapper.selectTopUsers();
    }

    @Cacheable(cacheNames = "eval:topTeams")
    public List<java.util.Map<String, Object>> getTopTeams() {
        return evaluationMapper.selectTopTeams();
    }

    @Cacheable(cacheNames = "eval:topClubs")
    public List<java.util.Map<String, Object>> getTopClubs() {
        return evaluationMapper.selectTopClubs();
    }

    public Double getAvgScore(Long userId) {
        return evaluationMapper.getAvgScore(userId);
    }

    public boolean hasEvaluated(Long orderId, Integer orderType, Long userId) {
        return evaluationMapper.countByOrder(orderId, orderType, userId) > 0;
    }
}
