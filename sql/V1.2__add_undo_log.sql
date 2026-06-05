-- Seata AT 模式 undo_log 表（每库一张）
-- 用于分布式事务回滚时还原数据
CREATE TABLE IF NOT EXISTS `undo_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `branch_id` BIGINT NOT NULL COMMENT '分支事务 ID',
    `xid` VARCHAR(100) NOT NULL COMMENT '全局事务 ID',
    `context` VARCHAR(128) NOT NULL,
    `rollback_info` LONGBLOB NOT NULL COMMENT '回滚所需的前镜像 JSON',
    `log_status` INT NOT NULL COMMENT '0=正常, 1=全局回滚完成',
    `log_created` DATETIME NOT NULL,
    `log_modified` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `ux_undo_log` (`xid`, `branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT 模式 Undo Log';
