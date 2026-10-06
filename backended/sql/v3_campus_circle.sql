-- ============================================
-- 校园互助学习资源共享系统 · v3 校园圈/社团小改
-- 对应 系统详细设计-2026-10-06.md §2.6
-- ============================================
USE campus_service;

-- 帖子加标签：讲座/竞赛/学术活动/求助 等，用于按兴趣推荐
ALTER TABLE `announcement`
  ADD COLUMN `tags` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '标签,逗号分隔(讲座/竞赛/学术活动/求助等)';

-- 社团加分类：支持学术类社团
ALTER TABLE `club_application`
  ADD COLUMN `category` VARCHAR(32) NOT NULL DEFAULT '其他' COMMENT '社团分类: 学术/文艺/体育/公益/其他';

-- type 取值扩充说明：0闲聊 1公告 2求助（求助帖纳入需求匹配主线）
-- 说明：announcement.type 已有 0/1，新增 2 不需要改表结构，仅约定取值。
