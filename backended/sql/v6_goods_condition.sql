-- 2026-10-12 闲置物品「成色」字段（借鉴闲鱼卡片角标）
-- 用法：Navicat 里对 campus_service 库执行。⚠️ 只能跑一次（再跑会报 Duplicate column）
-- 前置：后端已加 Goods.conditionLevel 字段

ALTER TABLE `goods`
  ADD COLUMN `condition_level` VARCHAR(16) NOT NULL DEFAULT '几乎全新'
  COMMENT '成色: 全新/几乎全新/轻微使用痕迹/明显使用痕迹';

-- 给已有数据制造差异，方便列表/详情看到不同角标
UPDATE `goods` SET `condition_level` = '全新'         WHERE `goods_id` % 4 = 0;
UPDATE `goods` SET `condition_level` = '轻微使用痕迹' WHERE `goods_id` % 4 = 2;
UPDATE `goods` SET `condition_level` = '明显使用痕迹' WHERE `goods_id` % 4 = 3;

-- 核对
-- SELECT goods_id, title, category, condition_level FROM `goods` ORDER BY goods_id;
