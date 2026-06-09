package com.campus.service.service;

import com.campus.service.entity.Comment;
import com.campus.service.entity.CommentLike;
import com.campus.service.entity.PostLike;
import com.campus.service.mapper.CommentLikeMapper;
import com.campus.service.mapper.CommentMapper;
import com.campus.service.mapper.PostLikeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CommunityService {

    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final CommentLikeMapper commentLikeMapper;

    public CommunityService(CommentMapper commentMapper, PostLikeMapper postLikeMapper, CommentLikeMapper commentLikeMapper) {
        this.commentMapper = commentMapper;
        this.postLikeMapper = postLikeMapper;
        this.commentLikeMapper = commentLikeMapper;
    }

    public List<Comment> getComments(Long postId) {
        return commentMapper.selectByPostId(postId);
    }

    public Comment addComment(Long postId, Long userId, String content, Long replyTo) {
        Comment c = new Comment();
        c.setPostId(postId);
        c.setUserId(userId);
        c.setContent(content);
        c.setReplyTo(replyTo);
        commentMapper.insert(c);
        return c;
    }

    // 帖子点赞/取消（toggle）
    @Transactional
    public boolean toggleLike(Long postId, Long userId) {
        if (postLikeMapper.exists(postId, userId) > 0) {
            // 已点赞 → 取消
            postLikeMapper.deleteByPostAndUser(postId, userId);
            return false;
        }
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        postLikeMapper.insert(like);
        return true;
    }

    public boolean toggleCommentPin(Long commentId, Long postId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null || !c.getPostId().equals(postId)) return false;
        c.setPinned(c.getPinned() != null && c.getPinned() == 1 ? 0 : 1);
        commentMapper.updateById(c);
        return true;
    }

    public Comment getCommentById(Long commentId) {
        return commentMapper.selectById(commentId);
    }

    public void deleteComment(Long commentId) {
        commentMapper.deleteById(commentId);
    }

    // 级联收集所有子评论ID
    public List<Long> collectDescendantIds(Long commentId) {
        List<Long> ids = new java.util.ArrayList<>();
        ids.add(commentId);
        List<Comment> children = commentMapper.selectByReplyTo(commentId);
        for (Comment child : children) {
            ids.addAll(collectDescendantIds(child.getId()));
        }
        return ids;
    }

    // 级联删除评论及其所有回复
    @Transactional
    public int deleteCommentCascade(Long commentId) {
        List<Long> ids = collectDescendantIds(commentId);
        for (Long id : ids) {
            commentMapper.deleteById(id);
        }
        return ids.size();
    }

    // 评论点赞/取消（toggle），原子更新计数
    @Transactional
    public boolean toggleCommentLike(Long commentId, Long userId) {
        Comment c = commentMapper.selectById(commentId);
        if (c == null) return false;
        if (commentLikeMapper.exists(commentId, userId) > 0) {
            // 已点赞 → 取消
            commentLikeMapper.deleteByCommentAndUser(commentId, userId);
            commentMapper.decrLikeCount(commentId);
            return false;
        }
        CommentLike like = new CommentLike();
        like.setCommentId(commentId);
        like.setUserId(userId);
        commentLikeMapper.insert(like);
        commentMapper.incrLikeCount(commentId);
        return true;
    }
}
