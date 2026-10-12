-- 2026-10-12 数据修正 + 演示数据补齐（非论文相关）
-- 用法：Navicat 里对 campus_service 库执行。可重复执行（都是按条件 UPDATE / 先删后插）。
-- 前置：先跑过 v4_test_data.sql

-- ============ 1. 修数据不一致 ============
-- 1.1 演示账号 userId=10 的信誉分是 NULL（首页显示的 99 只是登录时的本地缓存快照）
UPDATE `user` SET `credit_score` = 99 WHERE `user_id` = 10 AND `credit_score` IS NULL;

-- 1.2 孤儿数据：goods.user_id=2 在 user 表里不存在 → 挂到真实用户 12 名下
UPDATE `goods` SET `user_id` = 12 WHERE `user_id` = 2;

-- ============ 2. 补求购数据（goods_demand 是空的，求购页没内容）============
DELETE FROM `goods_demand` WHERE `title` IN ('求购一台二手显示器','求购《数据结构》教材','求购一个篮球');
INSERT INTO `goods_demand`
(`user_id`,`title`,`category`,`keyword`,`max_price`,`description`,`status`,`match_count`,`create_time`) VALUES
(12,'求购一台二手显示器','电子产品','显示器',600.00,'24 寸以上，无坏点即可，可自提',0,1,NOW()),
(10,'求购《数据结构》教材','书籍教材','数据结构 教材',40.00,'要严蔚敏版，有成色就行',0,2,NOW()),
(12,'求购一个篮球','运动健身','篮球',80.00,'7 号球，室内外都可以',0,0,NOW());

-- ============ 3. 补社团分类（club_application.category 全是"其他"，分类演示看不出效果）============
UPDATE `club_application` SET `category` = '学术' WHERE `club_name` = 'Java技术交流社';
UPDATE `club_application` SET `category` = '学术' WHERE `club_name` = 'Python技术交流社';
UPDATE `club_application` SET `category` = '文艺' WHERE `club_name` = '1';

-- ============ 4. 给校园圈帖子打标签（"推荐"页按兴趣标签匹配，没标签推不出东西）============
--    顺手把一条置为 type=2（求助帖），演示新增的求助帖类型
UPDATE `announcement` SET `tags` = '讲座,学术活动'  WHERE `title` = '元旦晚会通知';
UPDATE `announcement` SET `tags` = '学习,竞赛'      WHERE `title` = '高考物理';
UPDATE `announcement` SET `tags` = '学习,求助'      WHERE `title` = '下午考数学，好紧张睡不着觉';
UPDATE `announcement` SET `tags` = '求助,吐槽', `type` = 2 WHERE `title` = '九江学院热死了！';

-- ============ 5. 可选：撤销 2026-10-12 我做验收时下的那单（goods/24 现在是"已售出"）============
-- 想恢复就取消下面 3 行的注释再执行
-- UPDATE `goods` SET `status` = 0, `buyer_id` = NULL WHERE `goods_id` = 24;
-- DELETE FROM `orders` WHERE `order_id` = 2;
-- DELETE FROM `payment_log` WHERE `order_id` = 2;
