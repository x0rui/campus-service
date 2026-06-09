-- ============================================
-- 校园多服务平台 · 完整数据库建表脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS campus_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_service;

-- ============================================
-- 1. user 用户表
-- 用途: 存储微信登录用户信息，支持学生/社团管理员/系统管理员三种角色
-- ============================================
CREATE TABLE `user` (
    `user_id`     BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid`      VARCHAR(64)  NOT NULL COMMENT '微信openid',
    `nick_name`   VARCHAR(64)  DEFAULT '' COMMENT '昵称',
    `avatar_url`  VARCHAR(512) DEFAULT '' COMMENT '头像URL',
    `role`        TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0普通学生 1社团管理员 2系统管理员',
    `real_name`   VARCHAR(32)  DEFAULT '' COMMENT '真实姓名',
    `student_id`  VARCHAR(32)  DEFAULT '' COMMENT '学号',
    `phone`       VARCHAR(16)  DEFAULT '' COMMENT '手机号',
    `college`     VARCHAR(64)  DEFAULT '' COMMENT '学院',
    `major`       VARCHAR(64)  DEFAULT '' COMMENT '专业',
    `class_name`  VARCHAR(64)  DEFAULT '' COMMENT '班级',
    `age`         INT          DEFAULT NULL COMMENT '年龄',
    `gender`      VARCHAR(8)   DEFAULT '' COMMENT '性别',
    `hobbies`     VARCHAR(256) DEFAULT '' COMMENT '兴趣爱好,逗号分隔',
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. goods 闲置物品表
-- 用途: 学生发布的二手/闲置物品信息
-- 关系: user_id → user.user_id (发布者), buyer_id → user.user_id (买家)
-- ============================================
CREATE TABLE `goods` (
    `goods_id`     BIGINT NOT NULL AUTO_INCREMENT COMMENT '物品ID',
    `user_id`      BIGINT NOT NULL COMMENT '发布者ID',
    `title`        VARCHAR(128) NOT NULL COMMENT '标题',
    `description`  VARCHAR(1024) DEFAULT '' COMMENT '描述',
    `category`     VARCHAR(32) NOT NULL COMMENT '分类',
    `price`        DECIMAL(10,2) NOT NULL COMMENT '价格',
    `images`       VARCHAR(2048) DEFAULT '[]' COMMENT '图片JSON数组',
    `status`       TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0在售 1已售出 2已下架',
    `buyer_id`     BIGINT DEFAULT NULL COMMENT '买家ID',
    `browse_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`goods_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_buyer_id` (`buyer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲置物品表';

-- ============================================
-- 3. task 跑腿任务表
-- 用途: 学生发布的校园跑腿任务
-- 关系: publisher_id → user.user_id, taker_id → user.user_id
-- ============================================
CREATE TABLE `task` (
    `task_id`          BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `publisher_id`     BIGINT NOT NULL COMMENT '发布者ID',
    `taker_id`         BIGINT DEFAULT NULL COMMENT '接单者ID',
    `pickup_location`  VARCHAR(256) NOT NULL COMMENT '取件地点名称',
    `pickup_lat`       DECIMAL(10,6) DEFAULT NULL COMMENT '取件纬度',
    `pickup_lng`       DECIMAL(10,6) DEFAULT NULL COMMENT '取件经度',
    `delivery_location` VARCHAR(256) NOT NULL COMMENT '送达地点名称',
    `delivery_lat`     DECIMAL(10,6) DEFAULT NULL COMMENT '送达纬度',
    `delivery_lng`     DECIMAL(10,6) DEFAULT NULL COMMENT '送达经度',
    `fee`              DECIMAL(10,2) NOT NULL COMMENT '跑腿费',
    `deadline`         DATETIME DEFAULT NULL COMMENT '截止时间',
    `remark`           VARCHAR(512) DEFAULT '' COMMENT '备注',
    `task_type`        VARCHAR(32) DEFAULT '其他' COMMENT '任务类型',
    `status`           TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待接单 1已接单 2已完成 3已取消',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `take_time`        DATETIME DEFAULT NULL COMMENT '接单时间',
    `complete_time`    DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`task_id`),
    KEY `idx_publisher` (`publisher_id`),
    KEY `idx_taker` (`taker_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跑腿任务表';

-- ============================================
-- 4. message 私信消息表
-- 用途: WebSocket聊天消息持久化
-- 关系: sender_id → user.user_id, receiver_id → user.user_id
-- ============================================
CREATE TABLE `message` (
    `msg_id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id`  VARCHAR(64) NOT NULL COMMENT '会话ID (小ID_大ID)',
    `sender_id`   BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `content`     VARCHAR(2048) NOT NULL COMMENT '消息内容',
    `msg_type`    TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型: 0文字 1图片',
    `is_read`     TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`msg_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_sender_receiver` (`sender_id`, `receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信消息表';

-- ============================================
-- 5. evaluation 评价表
-- 用途: 交易/活动完成后的互评记录
-- 关系: evaluator_id → user.user_id, target_id → user.user_id
--       order_id 关联 goods/task/team/club_application
-- ============================================
CREATE TABLE `evaluation` (
    `eval_id`      BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`     BIGINT NOT NULL COMMENT '订单ID',
    `order_type`   TINYINT NOT NULL COMMENT '订单类型: 0跑腿 1闲置 2组局 3社团',
    `evaluator_id` BIGINT NOT NULL COMMENT '评价者ID',
    `target_id`    BIGINT NOT NULL COMMENT '被评价者ID',
    `score`        TINYINT NOT NULL COMMENT '评分1-5',
    `content`      VARCHAR(512) DEFAULT '' COMMENT '评价内容',
    `is_anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名: 0否 1是',
    `create_time`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`eval_id`),
    KEY `idx_target_id` (`target_id`),
    UNIQUE KEY `uk_order_evaluator` (`order_id`, `order_type`, `evaluator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================
-- 6. sensitive_word 敏感词表
-- 用途: 内容安全过滤，启动时加载到内存HashSet
-- ============================================
CREATE TABLE `sensitive_word` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `word`        VARCHAR(64) NOT NULL COMMENT '敏感词',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 初始化 18 个敏感词
INSERT INTO `sensitive_word` (`word`) VALUES
('代考'),('替考'),('作弊'),('答案'),('办证'),('刻章'),
('发票'),('套现'),('高利贷'),('赌博'),('色情'),('毒品'),
('枪支'),('弹药'),('裸聊'),('约炮'),('嫖娼'),('卖淫');

-- ============================================
-- 7. team 组局表
-- 用途: 学生发起的组队活动(学习/运动/旅游等)
-- 关系: user_id → user.user_id (发起人)
-- ============================================
CREATE TABLE `team` (
    `team_id`         BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT NOT NULL COMMENT '发起人ID',
    `title`           VARCHAR(128) NOT NULL COMMENT '标题',
    `description`     VARCHAR(1024) DEFAULT '' COMMENT '描述',
    `tag`             VARCHAR(32) NOT NULL DEFAULT '其他' COMMENT '标签: 学习/运动/旅游/游戏等',
    `max_members`     INT NOT NULL DEFAULT 10 COMMENT '人数上限',
    `min_members`     INT NOT NULL DEFAULT 1 COMMENT '最少成团人数',
    `current_members` INT NOT NULL DEFAULT 1 COMMENT '当前人数',
    `location`        VARCHAR(256) DEFAULT '' COMMENT '集合地点',
    `start_time`      DATETIME DEFAULT NULL COMMENT '开始时间',
    `end_time`        DATETIME DEFAULT NULL COMMENT '结束时间',
    `status`          TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0招募中 1已满 2已结束 3已取消',
    `sign_code`       VARCHAR(32) DEFAULT NULL COMMENT '签到码',
    `image`           VARCHAR(512) DEFAULT '' COMMENT '海报图片URL',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`team_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_tag` (`tag`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组局表';

-- ============================================
-- 8. team_join 组局成员表
-- 用途: 组局的加入申请和签到记录
-- 关系: team_id → team.team_id, user_id → user.user_id
-- ============================================
CREATE TABLE `team_join` (
    `join_id`     BIGINT NOT NULL AUTO_INCREMENT,
    `team_id`     BIGINT NOT NULL COMMENT '组局ID',
    `user_id`     BIGINT NOT NULL COMMENT '申请用户ID',
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1已通过 2已拒绝',
    `checked_in`  TINYINT NOT NULL DEFAULT 0 COMMENT '0未签到 1已签到',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`join_id`),
    UNIQUE KEY `uk_team_user` (`team_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组局成员表';

-- ============================================
-- 9. announcement 校园圈帖子/公告表
-- 用途: 校园圈(闲聊+公告)帖子内容
-- 关系: user_id → user.user_id
-- ============================================
CREATE TABLE `announcement` (
    `id`            BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT NOT NULL COMMENT '发布者ID',
    `club_name`     VARCHAR(64) DEFAULT '' COMMENT '社团/组织名称',
    `title`         VARCHAR(128) NOT NULL COMMENT '标题',
    `content`       TEXT COMMENT '正文内容',
    `image`         VARCHAR(512) DEFAULT '' COMMENT '海报图片URL',
    `location`      VARCHAR(256) DEFAULT '' COMMENT '活动地点',
    `event_time`    DATETIME DEFAULT NULL COMMENT '活动时间',
    `status`        TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1已结束',
    `type`          TINYINT NOT NULL DEFAULT 0 COMMENT '类型: 0闲聊 1公告',
    `like_count`    INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '评论数',
    `pinned`        TINYINT NOT NULL DEFAULT 0 COMMENT '0普通 1置顶',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校园圈帖子表';

-- ============================================
-- 10. comment 评论表
-- 用途: 校园圈帖子的评论(支持楼中楼回复)
-- 关系: post_id → announcement.id, user_id → user.user_id, reply_to → comment.id
-- ============================================
CREATE TABLE `comment` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `post_id`     BIGINT NOT NULL COMMENT '帖子ID',
    `user_id`     BIGINT NOT NULL COMMENT '评论者ID',
    `content`     VARCHAR(1024) NOT NULL COMMENT '内容',
    `reply_to`    BIGINT DEFAULT NULL COMMENT '回复哪条评论(NULL=顶层)',
    `pinned`      TINYINT NOT NULL DEFAULT 0 COMMENT '0普通 1置顶',
    `like_count`  INT NOT NULL DEFAULT 0 COMMENT '点赞数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_post` (`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- ============================================
-- 11. post_like 帖子点赞表
-- 用途: 校园圈帖子点赞记录
-- 关系: post_id → announcement.id, user_id → user.user_id
-- ============================================
CREATE TABLE `post_like` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `post_id`     BIGINT NOT NULL COMMENT '帖子ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_user` (`post_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子点赞表';

-- ============================================
-- 12. comment_like 评论点赞表
-- 用途: 评论点赞记录
-- 关系: comment_id → comment.id, user_id → user.user_id
-- ============================================
CREATE TABLE `comment_like` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `comment_id`  BIGINT NOT NULL COMMENT '评论ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_comment_user` (`comment_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论点赞表';

-- ============================================
-- 13. club_application 社团入驻申请表
-- 用途: 学生申请创建社团
-- 关系: user_id → user.user_id
-- ============================================
CREATE TABLE `club_application` (
    `app_id`      BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL COMMENT '申请人ID',
    `club_name`   VARCHAR(64) NOT NULL COMMENT '社团名称',
    `description` VARCHAR(512) DEFAULT '' COMMENT '简介',
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1通过 2拒绝',
    `reason`      VARCHAR(256) DEFAULT '' COMMENT '审核原因',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `audit_time`  DATETIME DEFAULT NULL COMMENT '审核时间',
    PRIMARY KEY (`app_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社团入驻申请表';

-- ============================================
-- 14. club_member 社团成员表
-- 用途: 社团的成员及审核管理
-- 关系: club_id → club_application.app_id, user_id → user.user_id
-- ============================================
CREATE TABLE `club_member` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `club_id`     BIGINT NOT NULL COMMENT '社团ID',
    `user_id`     BIGINT NOT NULL COMMENT '用户ID',
    `role`        TINYINT NOT NULL DEFAULT 0 COMMENT '0成员 1管理员',
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已拒绝',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_club_user` (`club_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社团成员表';

-- ============================================
-- 15. report 举报表
-- 用途: 用户举报不良内容
-- 关系: reporter_id → user.user_id, handler_id → user.user_id
-- ============================================
CREATE TABLE `report` (
    `report_id`       BIGINT NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    `reporter_id`     BIGINT NOT NULL COMMENT '举报人ID',
    `target_type`     VARCHAR(32) NOT NULL COMMENT '举报类型: goods/task/team/club/post/announcement/user',
    `target_id`       BIGINT NOT NULL COMMENT '被举报对象ID',
    `target_title`    VARCHAR(256) DEFAULT '' COMMENT '被举报对象标题',
    `reason`          VARCHAR(64) NOT NULL COMMENT '举报原因: 虚假信息/违规内容/恶意行为/其他',
    `description`     VARCHAR(1024) NOT NULL COMMENT '举报描述',
    `evidence_images` VARCHAR(2048) DEFAULT '[]' COMMENT '证据截图JSON数组',
    `status`          TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待处理 1已确认违规 2已驳回',
    `handler_id`      BIGINT DEFAULT NULL COMMENT '处理人ID',
    `handle_note`     VARCHAR(256) DEFAULT '' COMMENT '处理备注',
    `handle_time`     DATETIME DEFAULT NULL COMMENT '处理时间',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`report_id`),
    KEY `idx_reporter` (`reporter_id`),
    KEY `idx_status` (`status`),
    KEY `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报表';

-- ============================================
-- 16. operation_log 操作日志表
-- 用途: AOP切面自动记录关键业务操作
-- ============================================
CREATE TABLE `operation_log` (
    `log_id`      BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT DEFAULT NULL COMMENT '操作人ID',
    `operation`   VARCHAR(256) DEFAULT '' COMMENT '操作名称',
    `method`      VARCHAR(256) DEFAULT '' COMMENT '方法名',
    `params`      VARCHAR(1024) DEFAULT '' COMMENT '参数',
    `ip`          VARCHAR(64) DEFAULT '' COMMENT 'IP地址',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- ============================================
-- ER 图关系说明
-- ============================================
-- user ──< goods         (1:N, 发布)
-- user ──< goods         (1:N, 购买, buyer_id)
-- user ──< task          (1:N, 发布/接单)
-- user ──< message       (1:N, 发送/接收)
-- user ──< evaluation    (1:N, 评价/被评价)
-- user ──< team          (1:N, 发起组局)
-- user ──< team_join     (1:N, 加入组局)
-- user ──< announcement  (1:N, 发帖)
-- user ──< comment       (1:N, 评论)
-- user ──< club_application (1:N, 申请社团)
-- user ──< club_member   (1:N, 社团成员)
-- user ──< report        (1:N, 举报/处理)
-- team ──< team_join     (1:N, 成员关系)
-- announcement ──< comment    (1:N, 帖子的评论)
-- announcement ──< post_like  (1:N, 帖子点赞)
-- comment ──< comment_like    (1:N, 评论点赞)
-- comment ──< comment         (递归, 楼中楼回复)
-- club_application ──< club_member (1:N)
-- ============================================
