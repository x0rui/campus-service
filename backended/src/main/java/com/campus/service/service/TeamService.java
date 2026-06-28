package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.service.entity.Team;
import com.campus.service.entity.TeamJoin;
import com.campus.service.mapper.TeamJoinMapper;
import com.campus.service.mapper.TeamMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TeamService {

    private final TeamMapper teamMapper;
    private final TeamJoinMapper teamJoinMapper;
    private final NotificationService notificationService;

    public TeamService(TeamMapper teamMapper, TeamJoinMapper teamJoinMapper, NotificationService notificationService) {
        this.teamMapper = teamMapper;
        this.teamJoinMapper = teamJoinMapper;
        this.notificationService = notificationService;
    }

    public Team create(Team team) {
        // 服务端校验
        if (team.getMinMembers() != null && team.getMaxMembers() != null && team.getMinMembers() > team.getMaxMembers()) {
            throw new RuntimeException("最少成团人数不能大于上限");
        }
        if (team.getStartTime() != null && team.getEndTime() != null && team.getEndTime().isBefore(team.getStartTime())) {
            throw new RuntimeException("结束时间不能早于开始时间");
        }
        team.setStatus(0);
        team.setCurrentMembers(1);
        if (team.getTag() == null) team.setTag("其他");
        if (team.getMaxMembers() == null) team.setMaxMembers(10);
        if (team.getMinMembers() == null) team.setMinMembers(1);
        teamMapper.insert(team);
        TeamJoin join = new TeamJoin();
        join.setTeamId(team.getTeamId());
        join.setUserId(team.getUserId());
        join.setStatus(1);
        teamJoinMapper.insert(join);
        return team;
    }

    @Transactional
    public Team update(Long teamId, Long userId, Team updated) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) return null;
        if (!team.getUserId().equals(userId)) return null;
        if (team.getStatus() != 0) return null; // 仅招募中可编辑
        // 服务端校验
        if (updated.getMinMembers() != null && updated.getMaxMembers() != null && updated.getMinMembers() > updated.getMaxMembers()) {
            throw new RuntimeException("最少成团人数不能大于上限");
        }
        updated.setTeamId(teamId);
        updated.setUserId(userId);
        updated.setStatus(team.getStatus());
        updated.setCurrentMembers(team.getCurrentMembers());
        updated.setSignCode(team.getSignCode());
        updated.setCreateTime(team.getCreateTime());
        teamMapper.updateById(updated);
        return updated;
    }

    public List<Team> getActiveList(int page) {
        return teamMapper.selectActiveList(page * 10, 10);
    }

    public List<Team> getByTag(String tag) {
        return teamMapper.selectByTag(tag);
    }

    public List<Team> search(String keyword) {
        return teamMapper.searchByKeyword(keyword);
    }

    public Team getDetail(Long teamId) {
        return teamMapper.selectById(teamId);
    }

    public List<Team> getMyTeams(Long userId) {
        return teamMapper.selectByUserId(userId);
    }

    public List<Team> getJoinedTeams(Long userId) {
        List<TeamJoin> joins = teamJoinMapper.selectByUserId(userId);
        List<Team> teams = new ArrayList<>();
        for (TeamJoin j : joins) {
            if (!j.getUserId().equals(userId)) continue;
            if (j.getStatus() != 1) continue; // 只返回已通过的
            Team t = teamMapper.selectById(j.getTeamId());
            if (t != null && !t.getUserId().equals(userId)) teams.add(t); // 排除自己创建的
        }
        return teams;
    }

    // 取消组局（发起人，仅招募中）
    public boolean cancel(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || !team.getUserId().equals(userId) || team.getStatus() != 0) return false;
        team.setStatus(3);
        teamMapper.updateById(team);
        // 拒绝所有待审核申请
        teamJoinMapper.rejectAllPending(teamId);
        return true;
    }

    @Transactional
    public String applyJoin(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null) return "组局不存在";
        if (team.getStatus() != 0) return "该组局已不再招募";
        if (team.getUserId().equals(userId)) return "你是发起人，无需申请";
        TeamJoin existing = teamJoinMapper.selectOne(
                new LambdaQueryWrapper<TeamJoin>()
                        .eq(TeamJoin::getTeamId, teamId)
                        .eq(TeamJoin::getUserId, userId));
        if (existing != null) {
            if (existing.getStatus() == 1) return "你已在该组局中";
            if (existing.getStatus() == 0) return "已申请，请等待发起人审核";
            // 被拒绝的可以重新申请
            if (existing.getStatus() == 2) {
                existing.setStatus(0);
                teamJoinMapper.updateById(existing);
                return null;
            }
            return "你已被拒绝";
        }
        TeamJoin join = new TeamJoin();
        join.setTeamId(teamId);
        join.setUserId(userId);
        join.setStatus(0);
        teamJoinMapper.insert(join);
        return null;
    }

    @Transactional
    public String approveJoin(Long joinId, Long userId, boolean approved) {
        TeamJoin join = teamJoinMapper.selectById(joinId);
        if (join == null) return "申请不存在";
        Team team = teamMapper.selectById(join.getTeamId());
        if (team == null || !team.getUserId().equals(userId)) return "无权操作";
        if (team.getStatus() != 0) return "该组局已不再招募";
        if (join.getStatus() != 0) return "已处理";
        if (approved) {
            // 检查是否已满
            if (team.getCurrentMembers() >= team.getMaxMembers()) return "组局已满员";
            int rows = teamMapper.incrMember(team.getTeamId());
            if (rows == 0) return "组局已满员";
            join.setStatus(1);
            teamJoinMapper.updateById(join);
            teamMapper.autoFull(team.getTeamId());
            try {
                notificationService.send(join.getUserId(), 2, "组局申请已通过",
                        "你申请的组局「" + (team.getTitle() != null ? team.getTitle() : "组局活动") + "」已通过审核", team.getTeamId());
            } catch (Exception ignored) {}
        } else {
            join.setStatus(2);
            teamJoinMapper.updateById(join);
        }
        return null;
    }

    public List<TeamJoin> getPendingJoins(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || !team.getUserId().equals(userId)) return new ArrayList<>();
        return teamJoinMapper.selectList(
                new LambdaQueryWrapper<TeamJoin>()
                        .eq(TeamJoin::getTeamId, teamId)
                        .eq(TeamJoin::getStatus, 0));
    }

    public String generateSignCode(Long teamId, Long userId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || !team.getUserId().equals(userId)) return null;
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        team.setSignCode(code);
        teamMapper.updateById(team);
        return code;
    }

    public String checkIn(Long teamId, String code, Long userId) {
        if (code == null) return "请输入签到码";
        Team team = teamMapper.selectById(teamId);
        if (team == null) return "组局不存在";
        if (team.getStatus() != 0 && team.getStatus() != 1) return "该组局已不再活跃";
        if (!code.equals(team.getSignCode())) return "签到码错误";
        TeamJoin join = teamJoinMapper.selectOne(
                new LambdaQueryWrapper<TeamJoin>()
                        .eq(TeamJoin::getTeamId, teamId)
                        .eq(TeamJoin::getUserId, userId)
                        .eq(TeamJoin::getStatus, 1)); // 仅已通过的成员可签到
        if (join == null) return "你不是该组局的成员";
        if (join.getCheckedIn() != null && join.getCheckedIn() == 1) return "已签到过了";
        join.setCheckedIn(1);
        teamJoinMapper.updateById(join);
        return null;
    }

    public List<TeamJoin> getMembers(Long teamId) {
        return teamJoinMapper.selectMembers(teamId);
    }

    @Transactional
    public String kickMember(Long teamId, Long creatorId, Long targetUserId) {
        Team team = teamMapper.selectById(teamId);
        if (team == null || !team.getUserId().equals(creatorId)) return "无权操作";
        if (team.getStatus() != 0 && team.getStatus() != 1) return "该组局已不再活跃";
        if (targetUserId.equals(creatorId)) return "不能踢出自己";
        teamJoinMapper.delete(new LambdaQueryWrapper<TeamJoin>()
                .eq(TeamJoin::getTeamId, teamId).eq(TeamJoin::getUserId, targetUserId));
        teamMapper.decrMember(teamId);
        return null;
    }

    // 每小时检查过期组局
    @Scheduled(fixedRate = 3600000)
    public void autoProcessExpired() {
        int closed = teamMapper.autoCloseExpired();
        int cancelled = teamMapper.autoCancelExpired();
        if (closed > 0 || cancelled > 0) {
            System.out.println("组局自动处理: " + closed + "个已关闭, " + cancelled + "个已取消");
        }
    }
}
