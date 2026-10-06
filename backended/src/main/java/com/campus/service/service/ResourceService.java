package com.campus.service.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.service.entity.Resource;
import com.campus.service.entity.ResourceFavorite;
import com.campus.service.entity.ResourceRecord;
import com.campus.service.mapper.ResourceFavoriteMapper;
import com.campus.service.mapper.ResourceMapper;
import com.campus.service.mapper.ResourceRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceRecordMapper recordMapper;
    private final ResourceFavoriteMapper favoriteMapper;
    private final SensitiveWordService sensitiveWordService;

    public ResourceService(ResourceMapper resourceMapper, ResourceRecordMapper recordMapper,
                           ResourceFavoriteMapper favoriteMapper, SensitiveWordService sensitiveWordService) {
        this.resourceMapper = resourceMapper;
        this.recordMapper = recordMapper;
        this.favoriteMapper = favoriteMapper;
        this.sensitiveWordService = sensitiveWordService;
    }

    // 上传资料：先入库待审核，管理员通过后才公开
    public Resource publish(Resource r) {
        r.setStatus(0);
        r.setViewCount(0);
        r.setDownloadCount(0);
        r.setCollectCount(0);
        r.setScore(BigDecimal.ZERO);
        r.setRejectReason("");
        resourceMapper.insert(r);
        return r;
    }

    public List<String> checkSensitiveWords(String text) {
        return sensitiveWordService.check(text);
    }

    // 列表：只返回已通过，课程/类型/关键词三个条件都可选
    public List<Resource> list(String course, String type, String keyword, int page) {
        QueryWrapper<Resource> w = new QueryWrapper<>();
        w.eq("status", 1);
        if (notBlank(course)) w.eq("course", course);
        if (notBlank(type)) w.eq("resource_type", type);
        if (notBlank(keyword)) w.and(q -> q.like("title", keyword).or().like("description", keyword));
        w.orderByDesc("create_time");
        w.last("LIMIT " + (page * 10) + ", 10");
        return resourceMapper.selectList(w);
    }

    public List<String> getCourses() {
        return resourceMapper.selectCourses();
    }

    public Resource getDetail(Long id) {
        Resource r = resourceMapper.selectById(id);
        if (r != null) resourceMapper.incrViewCount(id);
        return r;
    }

    public List<Resource> getMy(Long userId) {
        return resourceMapper.selectList(new QueryWrapper<Resource>()
                .eq("user_id", userId).orderByDesc("create_time"));
    }

    // 下载：计数 + 留痕，返回带 fileUrl 的资料
    @Transactional
    public Resource download(Long id, Long userId) {
        Resource r = resourceMapper.selectById(id);
        if (r == null || r.getStatus() != 1) return null;
        resourceMapper.incrDownloadCount(id);
        ResourceRecord rec = new ResourceRecord();
        rec.setResourceId(id);
        rec.setUserId(userId);
        recordMapper.insert(rec);
        r.setDownloadCount(r.getDownloadCount() + 1);
        return r;
    }

    // 收藏切换：已收藏则取消，未收藏则收藏
    @Transactional
    public boolean toggleCollect(Long id, Long userId) {
        ResourceFavorite exist = favoriteMapper.selectOne(new QueryWrapper<ResourceFavorite>()
                .eq("resource_id", id).eq("user_id", userId));
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            resourceMapper.decrCollectCount(id);
            return false;
        }
        ResourceFavorite fav = new ResourceFavorite();
        fav.setResourceId(id);
        fav.setUserId(userId);
        favoriteMapper.insert(fav);
        resourceMapper.incrCollectCount(id);
        return true;
    }

    public boolean isFavorite(Long id, Long userId) {
        if (userId == null) return false;
        return favoriteMapper.selectCount(new QueryWrapper<ResourceFavorite>()
                .eq("resource_id", id).eq("user_id", userId)) > 0;
    }

    public List<Resource> getMyFavorite(Long userId) {
        return resourceMapper.selectFavoriteByUserId(userId);
    }

    // 管理员审核
    public boolean audit(Long id, Integer status, String reason) {
        Resource r = resourceMapper.selectById(id);
        if (r == null) return false;
        r.setStatus(status);
        r.setRejectReason(reason == null ? "" : reason);
        r.setAuditTime(LocalDateTime.now());
        return resourceMapper.updateById(r) > 0;
    }

    // 后台列表（可按状态筛）
    public List<Resource> adminList(Integer status, int page) {
        QueryWrapper<Resource> w = new QueryWrapper<>();
        if (status != null) w.eq("status", status);
        w.orderByDesc("create_time").last("LIMIT " + (page * 10) + ", 10");
        return resourceMapper.selectList(w);
    }

    // 上传者删除自己的资料
    public boolean remove(Long id, Long userId) {
        Resource r = resourceMapper.selectById(id);
        if (r == null || !r.getUserId().equals(userId)) return false;
        return resourceMapper.deleteById(id) > 0;
    }

    static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
