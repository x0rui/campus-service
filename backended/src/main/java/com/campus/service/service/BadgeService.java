package com.campus.service.service;

import com.campus.service.mapper.ClubApplicationMapper;
import com.campus.service.mapper.MessageMapper;
import com.campus.service.mapper.NotificationMapper;
import com.campus.service.mapper.ReportMapper;
import com.campus.service.mapper.TeamJoinMapper;
import org.springframework.stereotype.Service;

@Service
public class BadgeService {

    private final MessageMapper messageMapper;
    private final ClubApplicationMapper clubApplicationMapper;
    private final ReportMapper reportMapper;
    private final TeamJoinMapper teamJoinMapper;
    private final NotificationMapper notificationMapper;

    public BadgeService(MessageMapper messageMapper, ClubApplicationMapper clubApplicationMapper, ReportMapper reportMapper, TeamJoinMapper teamJoinMapper, NotificationMapper notificationMapper) {
        this.messageMapper = messageMapper;
        this.clubApplicationMapper = clubApplicationMapper;
        this.reportMapper = reportMapper;
        this.teamJoinMapper = teamJoinMapper;
        this.notificationMapper = notificationMapper;
    }

    public int getUnreadMessages(Long userId) {
        try {
            Long count = messageMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.Message>()
                    .eq(com.campus.service.entity.Message::getReceiverId, userId)
                    .eq(com.campus.service.entity.Message::getIsRead, 0));
            return count != null ? count.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    public int getPendingClubApps(Long userId) {
        try {
            Long count = clubApplicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.ClubApplication>()
                    .eq(com.campus.service.entity.ClubApplication::getUserId, userId)
                    .eq(com.campus.service.entity.ClubApplication::getStatus, 0));
            return count != null ? count.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    public int getHandledReports(Long userId, Long lastViewTime) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.Report> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.Report>()
                    .eq(com.campus.service.entity.Report::getReporterId, userId)
                    .ne(com.campus.service.entity.Report::getStatus, 0);
            if (lastViewTime != null && lastViewTime > 0) {
                java.time.LocalDateTime since = java.time.LocalDateTime.ofEpochSecond(
                    lastViewTime / 1000, 0, java.time.ZoneOffset.ofHours(8));
                qw.gt(com.campus.service.entity.Report::getHandleTime, since);
            } else {
                qw.gt(com.campus.service.entity.Report::getHandleTime,
                    java.time.LocalDateTime.now().minusHours(24));
            }
            Long count = reportMapper.selectCount(qw);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    public int getPendingClubChecks() {
        try {
            Long count = clubApplicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.ClubApplication>()
                    .eq(com.campus.service.entity.ClubApplication::getStatus, 0));
            return count != null ? count.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    public int getPendingReports() {
        try {
            Long count = reportMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.Report>()
                    .eq(com.campus.service.entity.Report::getStatus, 0));
            return count != null ? count.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    public int getPendingTeamJoins(Long userId) {
        try {
            // 我创建的组局中待审核的申请数
            Long count = teamJoinMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.campus.service.entity.TeamJoin>()
                    .eq(com.campus.service.entity.TeamJoin::getStatus, 0)
                    .inSql(com.campus.service.entity.TeamJoin::getTeamId,
                        "SELECT team_id FROM team WHERE user_id = " + userId));
            return count != null ? count.intValue() : 0;
        } catch (Exception e) { return 0; }
    }

    public int getUnreadNotifications(Long userId) {
        return notificationMapper.countUnread(userId);
    }
}
