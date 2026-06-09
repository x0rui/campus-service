-- ============================================
-- 校园闲置交易与跑腿平台 - 数据库初始化脚本
-- ============================================

CREATE DATABASE IF NOT EXISTS campus_service DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_service;

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `openid` VARCHAR(64) NOT NULL COMMENT '微信openid',
    `nick_name` VARCHAR(64) DEFAULT '' COMMENT '昵称',
    `avatar_url` VARCHAR(512) DEFAULT '' COMMENT '头像URL',
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '角色: 0普通学生 1社团管理员 2系统管理员',
    `real_name` VARCHAR(32) DEFAULT '' COMMENT '真实姓名',
    `student_id` VARCHAR(32) DEFAULT '' COMMENT '学号',
    `phone` VARCHAR(16) DEFAULT '' COMMENT '手机号',
    `college` VARCHAR(64) DEFAULT '' COMMENT '学院',
    `major` VARCHAR(64) DEFAULT '' COMMENT '专业',
    `class_name` VARCHAR(64) DEFAULT '' COMMENT '班级',
    `age` INT DEFAULT NULL COMMENT '年龄',
    `gender` VARCHAR(8) DEFAULT '' COMMENT '性别',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0正常 1禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 闲置物品表
DROP TABLE IF EXISTS `goods`;
CREATE TABLE `goods` (
    `goods_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '物品ID',
    `user_id` BIGINT NOT NULL COMMENT '发布者ID',
    `title` VARCHAR(128) NOT NULL COMMENT '标题',
    `description` VARCHAR(1024) DEFAULT '' COMMENT '描述',
    `category` VARCHAR(32) NOT NULL COMMENT '分类',
    `price` DECIMAL(10,2) NOT NULL COMMENT '价格',
    `images` VARCHAR(2048) DEFAULT '[]' COMMENT '图片JSON数组',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0在售 1已售出 2已下架',
    `browse_count` INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`goods_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='闲置物品表';

-- 跑腿任务表
DROP TABLE IF EXISTS `task`;
CREATE TABLE `task` (
    `task_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `publisher_id` BIGINT NOT NULL COMMENT '发布者ID',
    `taker_id` BIGINT DEFAULT NULL COMMENT '接单者ID',
    `pickup_location` VARCHAR(256) NOT NULL COMMENT '取件地点名称',
    `pickup_lat` DECIMAL(10,6) DEFAULT NULL COMMENT '取件纬度',
    `pickup_lng` DECIMAL(10,6) DEFAULT NULL COMMENT '取件经度',
    `delivery_location` VARCHAR(256) NOT NULL COMMENT '送达地点名称',
    `delivery_lat` DECIMAL(10,6) DEFAULT NULL COMMENT '送达纬度',
    `delivery_lng` DECIMAL(10,6) DEFAULT NULL COMMENT '送达经度',
    `fee` DECIMAL(10,2) NOT NULL COMMENT '跑腿费',
    `deadline` DATETIME DEFAULT NULL COMMENT '截止时间',
    `remark` VARCHAR(512) DEFAULT '' COMMENT '备注',
    `task_type` VARCHAR(32) DEFAULT '其他' COMMENT '任务类型: 快递/带饭/代买/其他',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待接单 1已接单 2已完成 3已取消',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `take_time` DATETIME DEFAULT NULL COMMENT '接单时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    PRIMARY KEY (`task_id`),
    KEY `idx_publisher` (`publisher_id`),
    KEY `idx_taker` (`taker_id`),
    KEY `idx_status` (`status`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='跑腿任务表';

-- 私信消息表
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
    `msg_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID (小ID_大ID)',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `receiver_id` BIGINT NOT NULL COMMENT '接收者ID',
    `content` VARCHAR(2048) NOT NULL COMMENT '消息内容',
    `msg_type` TINYINT NOT NULL DEFAULT 0 COMMENT '消息类型: 0文字 1图片',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`msg_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_sender_receiver` (`sender_id`, `receiver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信消息表';

-- 评价表
DROP TABLE IF EXISTS `evaluation`;
CREATE TABLE `evaluation` (
    `eval_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `order_type` TINYINT NOT NULL COMMENT '订单类型: 0跑腿 1闲置',
    `evaluator_id` BIGINT NOT NULL COMMENT '评价者ID',
    `target_id` BIGINT NOT NULL COMMENT '被评价者ID',
    `score` TINYINT NOT NULL COMMENT '评分1-5',
    `content` VARCHAR(512) DEFAULT '' COMMENT '评价内容',
    `is_anonymous` TINYINT NOT NULL DEFAULT 0 COMMENT '是否匿名: 0否 1是',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`eval_id`),
    KEY `idx_target_id` (`target_id`),
    KEY `idx_order` (`order_id`, `order_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- 敏感词表
DROP TABLE IF EXISTS `sensitive_word`;
CREATE TABLE `sensitive_word` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `word` VARCHAR(64) NOT NULL COMMENT '敏感词',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_word` (`word`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 初始化敏感词数据
INSERT INTO `sensitive_word` (`word`) VALUES
('代考'),('替考'),('作弊'),('答案'),('办证'),('刻章'),
('发票'),('套现'),('高利贷'),('赌博'),('色情'),('毒品'),
('枪支'),('弹药'),('裸聊'),('约炮'),('嫖娼'),('卖淫');

-- 初始化系统管理员
INSERT INTO `user` (`openid`, `nick_name`, `role`, `real_name`, `student_id`, `phone`, `status`) VALUES
('admin_openid_placeholder', '系统管理员', 2, '管理员', '000000', '13800000000', 0);
