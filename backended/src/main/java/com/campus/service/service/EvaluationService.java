package com.campus.service.service;

import com.campus.service.entity.Evaluation;
import com.campus.service.mapper.EvaluationMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;

    public EvaluationService(EvaluationMapper evaluationMapper) {
        this.evaluationMapper = evaluationMapper;
    }

    public Evaluation evaluate(Evaluation evaluation) {
        evaluationMapper.insert(evaluation);
        return evaluation;
    }

    public List<Evaluation> getUserEvaluations(Long userId) {
        return evaluationMapper.selectByTargetId(userId);
    }

    public List<Evaluation> getGivenEvaluations(Long userId) {
        return evaluationMapper.selectByEvaluatorId(userId);
    }

    public List<java.util.Map<String, Object>> getTopUsers() {
        return evaluationMapper.selectTopUsers();
    }

    public List<java.util.Map<String, Object>> getTopTeams() {
        return evaluationMapper.selectTopTeams();
    }

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
