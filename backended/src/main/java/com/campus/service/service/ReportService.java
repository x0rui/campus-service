package com.campus.service.service;

import com.campus.service.entity.Report;
import com.campus.service.mapper.ReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ReportService {

    private final ReportMapper reportMapper;
    private final NotificationService notificationService;

    public ReportService(ReportMapper reportMapper, NotificationService notificationService) {
        this.reportMapper = reportMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public Report create(Report r) {
        r.setStatus(0);
        r.setEvidenceImages(r.getEvidenceImages() != null ? r.getEvidenceImages() : "[]");
        reportMapper.insert(r);
        return r;
    }

    public List<Report> getMy(Long userId) {
        return reportMapper.selectByReporter(userId);
    }

    public List<Report> getAdminList(String targetType, Integer status, String keyword) {
        // 关键字搜索直接用 MyBatis-Plus 扩展
        List<Report> list;
        if (status != null) {
            if (targetType != null && !targetType.isEmpty() && !"all".equals(targetType)) {
                list = reportMapper.selectByTypeAndStatus(targetType, status);
            } else {
                list = reportMapper.selectByStatus(status);
            }
        } else {
            list = reportMapper.selectByStatus(0); // 默认待处理
        }

        // 关键字过滤（服务层简单处理）
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            list.removeIf(r -> !(r.getTargetTitle() != null && r.getTargetTitle().toLowerCase().contains(kw)
                    || r.getDescription() != null && r.getDescription().toLowerCase().contains(kw)
                    || r.getReason() != null && r.getReason().toLowerCase().contains(kw)));
        }
        return list;
    }

    @Transactional
    public boolean handle(Long reportId, int status, Long handlerId, String handleNote) {
        if (status != 1 && status != 2) return false;
        Report r = reportMapper.selectById(reportId);
        if (r == null || r.getStatus() != 0) return false;
        reportMapper.handleReport(reportId, status, handlerId, handleNote != null ? handleNote : "", LocalDateTime.now());

        // 给举报人发系统通知
        try {
            String result = status == 1 ? "已确认违规" : "已驳回";
            String content = "你举报的「" + (r.getTargetTitle() != null ? r.getTargetTitle() : r.getTargetType()) + "」" + result + "。" + (handleNote != null && !handleNote.isEmpty() ? " 处理备注：" + handleNote : "");
            notificationService.send(r.getReporterId(), 5, "举报处理结果", content, r.getReportId());
        } catch (Exception ignored) {}

        return true;
    }

    public Report getDetail(Long id) {
        return reportMapper.selectById(id);
    }
}
