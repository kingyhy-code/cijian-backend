-- 2026-05-24: 简化话题表 + 删除冗余灵感引用表
-- 注意：cijian-content 启动时会自动创建/更新表结构（MyBatis-Plus），此 SQL 仅用于手动对比

-- 1. topic 表：删除时间限制字段和状态字段
ALTER TABLE `topic`
  DROP COLUMN `start_time`,
  DROP COLUMN `end_time`,
  DROP COLUMN `status`;

-- 2. 删除冗余的 inspiration_ref 表（work.inspiration_from 已覆盖）
DROP TABLE IF EXISTS `inspiration_ref`;
