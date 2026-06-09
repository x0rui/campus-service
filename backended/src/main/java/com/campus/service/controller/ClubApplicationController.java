package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.ClubApplication;
import com.campus.service.entity.ClubMember;
import com.campus.service.entity.Message;
import com.campus.service.entity.User;
import com.campus.service.mapper.ClubApplicationMapper;
import com.campus.service.mapper.ClubMemberMapper;
import com.campus.service.mapper.MessageMapper;
import com.campus.service.service.UserService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/club")
public class ClubApplicationController {

    private final ClubApplicationMapper clubApplicationMapper;
    private final UserService userService;
    private final MessageMapper messageMapper;
    private final ClubMemberMapper clubMemberMapper;

    public ClubApplicationController(ClubApplicationMapper clubApplicationMapper, UserService userService, MessageMapper messageMapper, ClubMemberMapper clubMemberMapper) {
        this.clubApplicationMapper = clubApplicationMapper;
        this.userService = userService;
        this.messageMapper = messageMapper;
        this.clubMemberMapper = clubMemberMapper;
    }

    @OpLog("申请入驻社团")
    @PostMapping("/apply")
    public Result<?> apply(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = (Long) request.getAttribute("userId");
        String name = body.get("clubName");
        String desc = body.get("description");
        if (name == null || name.trim().isEmpty()) return Result.fail("请输入社团名称");
        // 检查重名
        Long dup = clubApplicationMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                .eq(ClubApplication::getClubName, name.trim())
                .eq(ClubApplication::getStatus, 1));
        if (dup != null && dup > 0) return Result.fail("该社团名已存在");
        // 检查是否已有待审核申请
        Long pending = clubApplicationMapper.selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                .eq(ClubApplication::getUserId, userId)
                .eq(ClubApplication::getStatus, 0));
        if (pending != null && pending > 0) return Result.fail("你已有待审核的申请");
        ClubApplication ca = new ClubApplication();
        ca.setUserId(userId);
        ca.setClubName(name.trim());
        ca.setDescription(desc != null ? desc : "");
        ca.setStatus(0);
        clubApplicationMapper.insert(ca);
        return Result.ok("申请已提交，等待审核");
    }

    @GetMapping("/pending")
    public Result<List<ClubApplication>> pending(HttpServletRequest request) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(403, "无权限");
        return Result.ok(clubApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                        .eq(ClubApplication::getStatus, 0).orderByDesc(ClubApplication::getCreateTime)));
    }

    @GetMapping("/my")
    public Result<List<ClubApplication>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(clubApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                        .eq(ClubApplication::getUserId, userId).orderByDesc(ClubApplication::getCreateTime)));
    }

    @OpLog("审核社团入驻")
    @PutMapping("/approve/{id}")
    public Result<?> approve(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer role = (Integer) request.getAttribute("role");
        if (role == null || role < 2) return Result.fail(403, "无权限");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        ClubApplication ca = clubApplicationMapper.selectById(id);
        if (ca == null) return Result.fail("申请不存在");
        if (ca.getStatus() != 0) return Result.fail("已处理");
        // 通过时检查重名
        String rejectReason = null;
        if (approved) {
            Long dup = clubApplicationMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                    .eq(ClubApplication::getClubName, ca.getClubName())
                    .eq(ClubApplication::getStatus, 1));
            if (dup != null && dup > 0) {
                approved = false;
                rejectReason = "社团名「" + ca.getClubName() + "」已被占用";
            }
        }
        if (!approved && rejectReason == null) {
            rejectReason = body.get("reason") != null ? body.get("reason").toString() : "";
        }
        ca.setStatus(approved ? 1 : 2);
        ca.setAuditTime(java.time.LocalDateTime.now());
        if (!approved && !rejectReason.isEmpty()) {
            ca.setReason(rejectReason);
        }
        clubApplicationMapper.updateById(ca);
        if (approved) {
            User u = userService.getUserById(ca.getUserId());
            if (u != null && u.getRole() < 1) { u.setRole(1); userService.updateUser(u); }
            try {
                ClubMember cm = new ClubMember();
                cm.setClubId(ca.getAppId());
                cm.setUserId(ca.getUserId());
                cm.setRole(1);
                cm.setStatus(1);
                clubMemberMapper.insert(cm);
            } catch (Exception ignored) {}
        }
        // 发系统通知给申请人
        try {
            String content;
            if (approved) {
                content = "你的社团「" + ca.getClubName() + "」入驻申请已通过，现在你是社团管理员了";
            } else {
                content = "你的社团「" + ca.getClubName() + "」入驻申请未通过";
                if (rejectReason != null && !rejectReason.isEmpty()) {
                    content += "，原因：" + rejectReason;
                }
            }
            Message msg = new Message();
            msg.setSessionId("1_" + ca.getUserId());
            msg.setSenderId(1L);
            msg.setReceiverId(ca.getUserId());
            msg.setContent(content);
            msg.setMsgType(0);
            msg.setIsRead(0);
            messageMapper.insert(msg);
        } catch (Exception ignored) {}
        return Result.ok(approved ? "已通过" : (rejectReason != null && !rejectReason.isEmpty() ? rejectReason : "已拒绝"));
    }

    // 我的社团
    @GetMapping("/my-club")
    public Result<List<ClubApplication>> myClub(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        return Result.ok(clubApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                        .eq(ClubApplication::getUserId, userId)
                        .eq(ClubApplication::getStatus, 1)));
    }

    // 社团广场列表
    @GetMapping("/square")
    public Result<List<ClubApplication>> square() {
        return Result.ok(clubApplicationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                        .eq(ClubApplication::getStatus, 1).orderByDesc(ClubApplication::getCreateTime)));
    }

    // 社团详情
    @GetMapping("/detail/{id}")
    public Result<ClubApplication> detail(@PathVariable Long id) {
        return Result.ok(clubApplicationMapper.selectById(id));
    }

    // 申请加入社团
    @PostMapping("/join/{clubId}")
    public Result<?> join(HttpServletRequest request, @PathVariable Long clubId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        ClubApplication ca = clubApplicationMapper.selectById(clubId);
        if (ca == null || ca.getStatus() != 1) return Result.fail("社团不存在或未通过审核");
        ClubMember existing = clubMemberMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getClubId, clubId)
                .eq(ClubMember::getUserId, userId));
        if (existing != null) {
            if (existing.getStatus() == 1) return Result.fail("你已是该社团成员");
            if (existing.getStatus() == 0) return Result.fail("已申请，等待审核");
            // 曾拒绝，重新申请
            existing.setStatus(0);
            existing.setRole(0);
            clubMemberMapper.updateById(existing);
            return Result.ok("申请已发送");
        }
        ClubMember cm = new ClubMember();
        cm.setClubId(clubId);
        cm.setUserId(userId);
        cm.setRole(0);
        cm.setStatus(0);
        try { clubMemberMapper.insert(cm); } catch (Exception e) { return Result.fail("操作失败"); }
        return Result.ok("申请已发送");
    }

    // 社团成员列表（始终包含创建者）
    @GetMapping("/members/{clubId}")
    public Result<List<ClubMember>> members(@PathVariable Long clubId) {
        try {
            List<ClubMember> list = clubMemberMapper.selectByClubId(clubId);
            if (list == null) list = new java.util.ArrayList<>();
            // 查创建者是否已在列表中
            ClubApplication ca = clubApplicationMapper.selectById(clubId);
            if (ca != null) {
                boolean hasAdmin = false;
                for (ClubMember m : list) {
                    if (m.getUserId() != null && m.getUserId().equals(ca.getUserId())) {
                        hasAdmin = true; break;
                    }
                }
                if (!hasAdmin) {
                    ClubMember admin = new ClubMember();
                    admin.setClubId(clubId);
                    admin.setUserId(ca.getUserId());
                    admin.setRole(1);
                    admin.setStatus(1);
                    try {
                        com.campus.service.entity.User u = userService.getUserById(ca.getUserId());
                        if (u != null) {
                            admin.setNickName(u.getNickName());
                            admin.setAvatarUrl(u.getAvatarUrl());
                            admin.setGender(u.getGender());
                            admin.setHobbies(u.getHobbies());
                            admin.setCollege(u.getCollege());
                        }
                    } catch(Exception ignored) {}
                    list.add(0, admin);
                }
            }
            return Result.ok(list);
        } catch (Exception e) {
            return Result.ok(java.util.Collections.emptyList());
        }
    }

    // 审核成员申请（需社团管理员或系统管理员）
    @PutMapping("/member/approve/{id}")
    public Result<?> approveMember(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        ClubMember m = clubMemberMapper.selectById(id);
        if (m == null) return Result.fail("申请不存在");
        // 权限：系统管理员(2) 或 该社团的创建者
        ClubApplication ca = clubApplicationMapper.selectById(m.getClubId());
        if (ca == null) return Result.fail("社团不存在");
        if ((role == null || role < 2) && !ca.getUserId().equals(userId)) return Result.fail(403, "无权限");
        boolean approved = Boolean.TRUE.equals(body.get("approved"));
        m.setStatus(approved ? 1 : 2);
        clubMemberMapper.updateById(m);
        return Result.ok(approved ? "已通过" : "已拒绝");
    }

    // 踢出成员（需社团管理员或系统管理员）
    @PostMapping("/member/kick/{id}")
    public Result<?> kickMember(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        ClubMember m = clubMemberMapper.selectById(id);
        if (m == null) return Result.fail("成员不存在");
        ClubApplication ca = clubApplicationMapper.selectById(m.getClubId());
        if (ca == null) return Result.fail("社团不存在");
        if ((role == null || role < 2) && !ca.getUserId().equals(userId)) return Result.fail(403, "无权限");
        if (m.getUserId().equals(userId)) return Result.fail("不能踢出自己");
        clubMemberMapper.deleteById(id);
        return Result.ok("已踢出");
    }

    // 检查用户社团状态：0=不是成员 1=已加入 2=待审核
    @GetMapping("/check-member/{clubId}")
    public Result<Integer> checkMember(HttpServletRequest request, @PathVariable Long clubId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        try {
            int member = clubMemberMapper.isMember(clubId, userId);
            if (member > 0) return Result.ok(1);
            java.util.List<ClubMember> pending = clubMemberMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubMember>()
                    .eq(ClubMember::getClubId, clubId)
                    .eq(ClubMember::getUserId, userId)
                    .eq(ClubMember::getStatus, 0));
            if (pending != null && !pending.isEmpty()) return Result.ok(2);
        } catch (Exception ignored) { /* club_member 表可能未创建 */ }
        // 兜底：如果用户是社团创建者，算已有成员
        ClubApplication ca = clubApplicationMapper.selectById(clubId);
        if (ca != null && ca.getUserId().equals(userId)) return Result.ok(1);
        return Result.ok(0);
    }
}
