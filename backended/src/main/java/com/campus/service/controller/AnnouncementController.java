package com.campus.service.controller;

import com.campus.service.annotation.OpLog;
import com.campus.service.dto.Result;
import com.campus.service.entity.Announcement;
import com.campus.service.entity.Comment;
import com.campus.service.entity.ClubApplication;
import com.campus.service.mapper.ClubApplicationMapper;
import com.campus.service.service.AnnouncementService;
import com.campus.service.service.CommunityService;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final CommunityService communityService;
    private final ClubApplicationMapper clubApplicationMapper;

    public AnnouncementController(AnnouncementService announcementService, CommunityService communityService,
                                   ClubApplicationMapper clubApplicationMapper) {
        this.announcementService = announcementService;
        this.communityService = communityService;
        this.clubApplicationMapper = clubApplicationMapper;
    }

    @OpLog("发布帖子/公告")
    @PostMapping("/create")
    public Result<Announcement> create(HttpServletRequest request, @RequestBody Announcement a) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        if (userId == null) return Result.fail(401, "请先登录");
        // 公告只有社团管理员及以上可发布
        if (a.getType() != null && a.getType() == 1 && (role == null || role < 1))
            return Result.fail("仅社团管理员及以上可发布公告");
        // 发公告时校验 clubName 是否属于该用户管理的社团
        if (a.getClubName() != null && !a.getClubName().isEmpty() && a.getType() != null && a.getType() == 1) {
            // 系统管理员(role=2)可以以任何社团名发公告
            if (role == null || role < 2) {
                Long count = clubApplicationMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ClubApplication>()
                        .eq(ClubApplication::getUserId, userId)
                        .eq(ClubApplication::getClubName, a.getClubName())
                        .eq(ClubApplication::getStatus, 1));
                if (count == null || count == 0)
                    return Result.fail("你不是该社团的管理员，不能以该社团名义发公告");
            }
        }
        a.setUserId(userId);
        return Result.ok(announcementService.create(a));
    }

    @GetMapping("/list")
    public Result<List<Announcement>> list(@RequestParam(defaultValue = "0") int type,
                                           @RequestParam(defaultValue = "0") int page) {
        return Result.ok(announcementService.getByType(type, page));
    }

    @GetMapping("/all")
    public Result<List<Announcement>> all() {
        return Result.ok(announcementService.getLatest());
    }

    @GetMapping("/detail/{id}")
    public Result<Announcement> detail(@PathVariable Long id) {
        return Result.ok(announcementService.getDetail(id));
    }

    @GetMapping("/my")
    public Result<List<Announcement>> my(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(announcementService.getMy(userId));
    }

    @PostMapping("/like/{id}")
    public Result<Boolean> like(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        boolean liked = announcementService.toggleLike(id, userId);
        return Result.ok(liked);
    }

    @OpLog("评论")
    @PostMapping("/comment/{id}")
    public Result<Comment> comment(HttpServletRequest request, @PathVariable Long id, @RequestBody Comment c) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Comment result = communityService.addComment(id, userId, c.getContent(), c.getReplyTo());
        announcementService.incrCommentCount(id);
        return Result.ok(result);
    }

    @PostMapping("/comment/like/{commentId}")
    public Result<?> commentLike(HttpServletRequest request, @PathVariable Long commentId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        boolean ok = communityService.toggleCommentLike(commentId, userId);
        return ok ? Result.ok("点赞成功") : Result.fail("已点赞");
    }

    @GetMapping("/comments/{id}")
    public Result<List<Comment>> comments(@PathVariable Long id) {
        return Result.ok(communityService.getComments(id));
    }

    @OpLog("置顶评论")
    @PostMapping("/comment/pin/{commentId}")
    public Result<?> pinComment(HttpServletRequest request, @PathVariable Long commentId,
                                 @RequestParam Long postId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        // 只有帖子作者可以置顶评论
        Announcement post = announcementService.getDetail(postId);
        if (post == null || !post.getUserId().equals(userId))
            return Result.fail("仅帖子作者可置顶评论");
        boolean ok = communityService.toggleCommentPin(commentId, postId);
        return ok ? Result.ok("操作成功") : Result.fail("评论不存在");
    }

    @OpLog("删除帖子")
    @DeleteMapping("/delete/{id}")
    public Result<?> delete(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        if (userId == null) return Result.fail(401, "请先登录");
        boolean ok = announcementService.deleteById(id, userId, role);
        return ok ? Result.ok("已删除") : Result.fail("无权操作");
    }

    @OpLog("删除评论")
    @DeleteMapping("/comment/{commentId}")
    public Result<?> deleteComment(HttpServletRequest request, @PathVariable Long commentId) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.fail(401, "请先登录");
        Comment c = communityService.getCommentById(commentId);
        if (c == null) return Result.fail("评论不存在");
        // 评论作者或帖子作者可以删除
        if (!c.getUserId().equals(userId)) {
            Announcement post = announcementService.getDetail(c.getPostId());
            if (post == null || !post.getUserId().equals(userId))
                return Result.fail("无权删除该评论");
        }
        int deleted = communityService.deleteCommentCascade(commentId);
        for (int i = 0; i < deleted; i++) {
            announcementService.decrCommentCount(c.getPostId());
        }
        return Result.ok("已删除");
    }

    @OpLog("置顶帖子")
    @PostMapping("/pin/{id}")
    public Result<?> pin(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        Integer role = (Integer) request.getAttribute("role");
        if (userId == null) return Result.fail(401, "请先登录");
        boolean ok = announcementService.pin(id, userId, role);
        return ok ? Result.ok() : Result.fail("无权操作");
    }
}
