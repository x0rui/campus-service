package com.campus.service.service;

import com.campus.service.entity.Announcement;
import com.campus.service.mapper.AnnouncementMapper;
import com.campus.service.mapper.CommentMapper;
import com.campus.service.mapper.CommentLikeMapper;
import com.campus.service.mapper.PostLikeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class AnnouncementService {

    private final AnnouncementMapper announcementMapper;
    private final CommunityService communityService;
    private final CommentMapper commentMapper;
    private final CommentLikeMapper commentLikeMapper;
    private final PostLikeMapper postLikeMapper;

    public AnnouncementService(AnnouncementMapper announcementMapper, CommunityService communityService,
                               CommentMapper commentMapper, CommentLikeMapper commentLikeMapper, PostLikeMapper postLikeMapper) {
        this.announcementMapper = announcementMapper;
        this.communityService = communityService;
        this.commentMapper = commentMapper;
        this.commentLikeMapper = commentLikeMapper;
        this.postLikeMapper = postLikeMapper;
    }

    public Announcement create(Announcement a) {
        a.setStatus(0);
        a.setType(a.getType() != null ? a.getType() : 0);
        a.setLikeCount(0);
        a.setCommentCount(0);
        a.setPinned(0);
        announcementMapper.insert(a);
        return a;
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
