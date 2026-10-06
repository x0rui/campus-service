package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.entity.Announcement;
import com.campus.service.entity.ClubApplication;
import com.campus.service.entity.ClubMember;
import com.campus.service.entity.User;
import com.campus.service.mapper.AnnouncementMapper;
import com.campus.service.mapper.ClubApplicationMapper;
import com.campus.service.mapper.ClubMemberMapper;
import com.campus.service.mapper.CommentMapper;
import com.campus.service.mapper.CommentLikeMapper;
import com.campus.service.mapper.PostLikeMapper;
import com.campus.service.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final CommunityService communityService;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final PostLikeMapper postLikeMapper;
    private final ClubApplicationMapper clubApplicationMapper;
    private final ClubMemberMapper clubMemberMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

    public AnnouncementService(AnnouncementMapper announcementMapper, CommunityService communityService,
                               CommentMapper commentMapper, CommentLikeMapper commentLikeMapper, PostLikeMapper postLikeMapper,
                               ClubApplicationMapper clubApplicationMapper, ClubMemberMapper clubMemberMapper,
                               UserMapper userMapper, NotificationService notificationService) {
        this.announcementMapper = announcementMapper;
        this.communityService = communityService;
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.postLikeMapper = postLikeMapper;
        this.clubApplicationMapper = clubApplicationMapper;
        this.clubMemberMapper = clubMemberMapper;
        this.userMapper = userMapper;
        this.notificationService = notificationService;
    }

    public Announcement create(Announcement a) {
        a.setStatus(0);
        a.setType(a.getType() != null ? a.getType() : 0);
        a.setLikeCount(0);
        a.setCommentCount(0);
        a.setPinned(0);
        if (a.getTags() == null) a.setTags("");
        announcementMapper.insert(a);

        // 社团公告不进公共信息流，定向推送给本社团成员，减少无关打扰
        if (a.getType() == 1 && a.getClubName() != null && !a.getClubName().trim().isEmpty()) {
            pushToClubMembers(a);
        }
        return a;
    }

    private void pushToClubMembers(Announcement a) {
        ClubApplication club = clubApplicationMapper.selectOne(new LambdaQueryWrapper<ClubApplication>()
                .eq(ClubApplication::getClubName, a.getClubName())
                .eq(ClubApplication::getStatus, 1)
                .last("LIMIT 1"));
        if (club == null) return;
        List<ClubMember> members = clubMemberMapper.selectList(new LambdaQueryWrapper<ClubMember>()
                .eq(ClubMember::getClubId, club.getAppId())
                .eq(ClubMember::getStatus, 1));
        for (ClubMember m : members) {
            if (m.getUserId() == null || m.getUserId().equals(a.getUserId())) continue;
            notificationService.send(m.getUserId(), 3, "社团公告",
                    a.getClubName() + "：" + a.getTitle(), a.getId());
        }
    }

    // 按帖子标签 + 发帖人兴趣标签，把帖子推给可能感兴趣的同学
    public List<Announcement> recommend(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null || u.getHobbies() == null) return new ArrayList<>();
        List<String> hobbies = new ArrayList<>();
        for (String h : u.getHobbies().split("[,，、\\s]+")) {
            if (h != null && !h.trim().isEmpty()) hobbies.add(h.trim());
        }
        if (hobbies.isEmpty()) return new ArrayList<>();

        QueryWrapper<Announcement> w = new QueryWrapper<>();
        w.eq("status", 0);
        w.and(q -> {
            boolean[] first = {true};
            for (String h : hobbies) {
                if (first[0]) { q.like("tags", h); first[0] = false; }
                else q.or().like("tags", h);
            }
        });
        w.orderByDesc("create_time").last("LIMIT 20");
        return announcementMapper.selectList(w);
    }

    public List<Announcement> getLatest() {
        return announcementMapper.selectLatest(50);
    }

    public List<Announcement> getByType(int type) {
        return announcementMapper.selectByType(type);
    }

    public List<Announcement> getByType(int type, int page) {
        return announcementMapper.selectByTypePage(type, page * 10, 10);
    }

    public Announcement getDetail(Long id) {
        return announcementMapper.selectById(id);
    }

    public List<Announcement> getMy(Long userId) {
        return announcementMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getUserId, userId).orderByDesc(Announcement::getCreateTime));
    }

    // 点赞/取消（toggle），原子更新计数
    @Transactional
    public boolean toggleLike(Long id, Long userId) {
        boolean liked = communityService.toggleLike(id, userId);
        if (liked) {
            announcementMapper.incrLikeCount(id);
        } else {
            announcementMapper.decrLikeCount(id);
        }
        return liked;
    }

    // 原子增加评论数
    public void incrCommentCount(Long id) {
        announcementMapper.incrCommentCount(id);
    }

    public void decrCommentCount(Long id) {
        announcementMapper.decrCommentCount(id);
    }

    // 置顶（作者或管理员）
    public boolean pin(Long id, Long userId, Integer role) {
        Announcement a = announcementMapper.selectById(id);
        if (a == null) return false;
        // 作者 或 系统管理员(role>=2) 可操作
        if (!a.getUserId().equals(userId) && (role == null || role < 2)) return false;
        a.setPinned(a.getPinned() != null && a.getPinned() == 1 ? 0 : 1);
        announcementMapper.updateById(a);
        return true;
    }

    // 级联删除帖子、评论、点赞
    @Transactional
    public boolean deleteById(Long id, Long userId, Integer role) {
        Announcement post = announcementMapper.selectById(id);
        if (post == null) return false;
        // 作者 或 系统管理员可删除
        if (!post.getUserId().equals(userId) && (role == null || role < 2)) return false;
        // 先删评论点赞，再删评论，再删帖子点赞，最后删帖子
        commentLikeMapper.deleteByPostId(id);
        commentMapper.deleteByPostId(id);
        postLikeMapper.deleteByPostId(id);
        announcementMapper.deleteById(id);
        return true;
    }
}
