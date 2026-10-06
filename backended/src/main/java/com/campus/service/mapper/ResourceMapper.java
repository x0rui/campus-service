package com.campus.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.service.entity.Resource;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;

public interface ResourceMapper extends BaseMapper<Resource> {

    // 课程去重列表（筛选下拉用）
    @Select("SELECT DISTINCT course FROM `resource` WHERE status = 1 ORDER BY course")
    List<String> selectCourses();

    // 资源匹配: 上传过某课程的资料的所有用户（求资源时推给这些人）
    @Select("SELECT DISTINCT user_id FROM `resource` WHERE status = 1 AND course = #{course}")
    List<Long> selectUserIdsByCourse(@Param("course") String course);

    // 原子计数，避免读-改-写竞态
    @Update("UPDATE `resource` SET view_count = view_count + 1 WHERE resource_id = #{id}")
    int incrViewCount(@Param("id") Long id);

    @Update("UPDATE `resource` SET download_count = download_count + 1 WHERE resource_id = #{id}")
    int incrDownloadCount(@Param("id") Long id);

    @Update("UPDATE `resource` SET collect_count = collect_count + 1 WHERE resource_id = #{id}")
    int incrCollectCount(@Param("id") Long id);

    @Update("UPDATE `resource` SET collect_count = collect_count - 1 WHERE resource_id = #{id} AND collect_count > 0")
    int decrCollectCount(@Param("id") Long id);

    // 某用户收藏过的资料
    @Select("SELECT r.* FROM `resource` r JOIN resource_favorite f ON r.resource_id = f.resource_id " +
            "WHERE f.user_id = #{userId} ORDER BY f.create_time DESC")
    List<Resource> selectFavoriteByUserId(@Param("userId") Long userId);
}
