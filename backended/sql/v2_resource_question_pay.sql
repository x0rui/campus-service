-- ============================================
-- 校园互助学习资源共享系统 · v2 新增表脚本
-- 对应 系统详细设计-2026-10-06.md 第一章
-- 在 campus_service 库执行；执行前建议先备份
-- ============================================
USE campus_service;

-- ============================================
-- 18. resource 学习资料表
-- 关系: user_id → user.user_id (上传者)
-- ============================================
CREATE TABLE IF NOT EXISTS `resource` (
    `resource_id`    BIGINT NOT NULL AUTO_INCREMENT COMMENT '资料ID',
    `user_id`        BIGINT NOT NULL COMMENT '上传者ID',
    `title`          VARCHAR(128) NOT NULL COMMENT '资料标题',
    `description`    VARCHAR(1024) DEFAULT '' COMMENT '资料说明',
    `course`         VARCHAR(64)  NOT NULL COMMENT '课程名称(第一维度)',
    `resource_type`  VARCHAR(32)  NOT NULL COMMENT '资料类型(第二维度): 课件/笔记/真题/代码/其他',
    `file_name`      VARCHAR(256) DEFAULT '' COMMENT '原始文件名',
    `file_url`       VARCHAR(512) NOT NULL COMMENT 'OSS文件URL',
    `file_ext`       VARCHAR(16)  DEFAULT '' COMMENT '扩展名 pdf/ppt/docx/mp4',
    `file_size`      BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    `view_count`     INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `download_count` INT NOT NULL DEFAULT 0 COMMENT '下载次数',
    `collect_count`  INT NOT NULL DEFAULT 0 COMMENT '收藏次数',
    `score`          DECIMAL(3,1) NOT NULL DEFAULT 0 COMMENT '平均评分0-5',
    `status`         TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待审核 1已通过 2已拒绝',
    `reject_reason`  VARCHAR(256) DEFAULT '' COMMENT '驳回原因',
    `create_time`    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `audit_time`     DATETIME DEFAULT NULL COMMENT '审核时间',
    PRIMARY KEY (`resource_id`),
    KEY `idx_user` (`user_id`), KEY `idx_course` (`course`),
    KEY `idx_type` (`resource_type`), KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习资料表';

-- ============================================
-- 19. resource_demand 求资源表
-- ============================================
CREATE TABLE IF NOT EXISTS `resource_demand` (
    `demand_id`     BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT NOT NULL COMMENT '求资源者ID',
    `title`         VARCHAR(128) NOT NULL COMMENT '需求标题: 求《数据结构》课件',
    `course`        VARCHAR(64) DEFAULT '' COMMENT '课程',
    `resource_type` VARCHAR(32) DEFAULT '' COMMENT '期望资料类型',
    `keyword`       VARCHAR(128) DEFAULT '' COMMENT '关键词',
    `description`   VARCHAR(1024) DEFAULT '' COMMENT '补充说明',
    `status`        TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0进行中 1已解决 2已关闭',
    `match_count`   INT NOT NULL DEFAULT 0 COMMENT '发布时匹配到的资料数',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`demand_id`),
    KEY `idx_user` (`user_id`), KEY `idx_course` (`course`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求资源表';

-- ============================================
-- 20. resource_record 资料下载记录表(只追加)
-- ============================================
CREATE TABLE IF NOT EXISTS `resource_record` (
    `record_id`   BIGINT NOT NULL AUTO_INCREMENT,
    `resource_id` BIGINT NOT NULL,
    `user_id`     BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`record_id`),
    KEY `idx_resource` (`resource_id`), KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料下载记录表';

-- ============================================
-- 21. resource_favorite 资料收藏表(唯一, 可取消)
-- ============================================
CREATE TABLE IF NOT EXISTS `resource_favorite` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `resource_id` BIGINT NOT NULL,
    `user_id`     BIGINT NOT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_resource_user` (`resource_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资料收藏表';

-- ============================================
-- 22. question 题目表
-- ============================================
CREATE TABLE IF NOT EXISTS `question` (
    `question_id` BIGINT NOT NULL AUTO_INCREMENT,
    `subject`     VARCHAR(64) NOT NULL COMMENT '科目: 数据结构/英语四级/考研数学',
    `chapter`     VARCHAR(64) DEFAULT '' COMMENT '章节/知识点(错题本反向推荐用)',
    `q_type`      TINYINT NOT NULL DEFAULT 0 COMMENT '题型: 0单选 1多选 2判断 3填空 4主观',
    `content`     TEXT NOT NULL COMMENT '题干',
    `options`     VARCHAR(2048) DEFAULT '[]' COMMENT '选项JSON数组, 选择题用',
    `answer`      VARCHAR(512) NOT NULL COMMENT '参考答案: 选择题如"A", 多选"AB"',
    `analysis`    VARCHAR(1024) DEFAULT '' COMMENT '答案解析',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`question_id`),
    KEY `idx_subject_chapter` (`subject`, `chapter`), KEY `idx_type` (`q_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';

-- ============================================
-- 23. question_record 答题记录表
-- ============================================
CREATE TABLE IF NOT EXISTS `question_record` (
    `record_id`   BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `user_answer` VARCHAR(512) DEFAULT '' COMMENT '用户作答',
    `is_correct`  TINYINT NOT NULL DEFAULT 0 COMMENT '0答错/未判分 1答对(主观题恒0)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`record_id`),
    KEY `idx_user` (`user_id`), KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录表';

-- ============================================
-- 24. question_wrong 错题本表(唯一)
-- ============================================
CREATE TABLE IF NOT EXISTS `question_wrong` (
    `id`          BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `wrong_times` INT NOT NULL DEFAULT 1 COMMENT '答错次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_question` (`user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- ============================================
-- 25. goods_demand 求购表
-- ============================================
CREATE TABLE IF NOT EXISTS `goods_demand` (
    `demand_id`   BIGINT NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT NOT NULL COMMENT '求购者ID',
    `title`       VARCHAR(128) NOT NULL COMMENT '求购标题',
    `category`    VARCHAR(32) DEFAULT '' COMMENT '期望分类',
    `keyword`     VARCHAR(128) DEFAULT '' COMMENT '关键词',
    `max_price`   DECIMAL(10,2) DEFAULT NULL COMMENT '可接受最高价',
    `description` VARCHAR(1024) DEFAULT '' COMMENT '补充说明',
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0进行中 1已找到 2已关闭',
    `match_count` INT NOT NULL DEFAULT 0 COMMENT '发布时匹配到的在售物品数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`demand_id`),
    KEY `idx_user` (`user_id`), KEY `idx_category` (`category`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求购表';

-- ============================================
-- 26. orders 闲置交易订单表 (order 是保留字, 表名用复数)
-- ============================================
CREATE TABLE IF NOT EXISTS `orders` (
    `order_id`    BIGINT NOT NULL AUTO_INCREMENT,
    `order_no`    VARCHAR(32) NOT NULL COMMENT '订单号(时间戳+随机)',
    `goods_id`    BIGINT NOT NULL COMMENT '物品ID',
    `buyer_id`    BIGINT NOT NULL COMMENT '买家ID',
    `seller_id`   BIGINT NOT NULL COMMENT '卖家ID',
    `amount`      DECIMAL(10,2) NOT NULL COMMENT '订单金额',
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待付款 1已付款 2待收货 3已完成 4已取消',
    `pay_channel` VARCHAR(16) DEFAULT '' COMMENT '支付渠道: wechat/alipay(模拟)',
    `pay_time`    DATETIME DEFAULT NULL COMMENT '付款时间',
    `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `cancel_time` DATETIME DEFAULT NULL COMMENT '取消时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`order_id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_buyer` (`buyer_id`), KEY `idx_seller` (`seller_id`),
    KEY `idx_goods` (`goods_id`), KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲置交易订单表';

-- ============================================
-- 27. payment_log 支付流水表
-- ============================================
CREATE TABLE IF NOT EXISTS `payment_log` (
    `log_id`      BIGINT NOT NULL AUTO_INCREMENT,
    `order_id`    BIGINT NOT NULL,
    `order_no`    VARCHAR(32) NOT NULL,
    `prepay_id`   VARCHAR(64) DEFAULT '' COMMENT '模拟预支付ID',
    `channel`     VARCHAR(16) DEFAULT '' COMMENT '支付渠道',
    `amount`      DECIMAL(10,2) NOT NULL,
    `status`      TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0已创建 1成功 2失败',
    `trade_no`    VARCHAR(64) DEFAULT '' COMMENT '模拟第三方交易号',
    `notify_body` VARCHAR(1024) DEFAULT '' COMMENT '回调报文(留痕)',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`log_id`),
    KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ============================================
-- 现有表改动: 枚举取值扩充（只改注释说明，不动结构）
-- ⚠️ MODIFY COLUMN 会丢属性，类型/字符集/排序规则/NOT NULL/COMMENT 必须写全
-- ============================================
ALTER TABLE `evaluation` MODIFY COLUMN `order_type` TINYINT NOT NULL
  COMMENT '订单类型: 0跑腿 1闲置 2组局 3社团 4学习资源';

ALTER TABLE `report` MODIFY COLUMN `target_type` VARCHAR(32)
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL
  COMMENT '举报类型: goods/task/team/post/announcement/user/resource';

ALTER TABLE `notification` MODIFY COLUMN `type` TINYINT NOT NULL DEFAULT 0
  COMMENT '类型: 0系统 1任务 2组局 3社团 4闲置 5举报 6学习资源 7支付';

-- ============================================
-- 新增关系
-- user ──< resource          (1:N, 上传)
-- user ──< resource_demand   (1:N, 求资源)
-- resource ──< resource_record   (1:N, 下载)
-- resource ──< resource_favorite (1:N, 收藏)
-- user ──< question_record   (1:N, 答题)
-- user ──< question_wrong    (1:N, 错题)
-- user ──< goods_demand      (1:N, 求购)
-- user ──< orders            (1:N, 买家/卖家)
-- orders ──< payment_log     (1:N, 支付流水)
-- ============================================
