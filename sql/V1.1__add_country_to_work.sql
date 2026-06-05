ALTER TABLE `work`
  ADD COLUMN `country` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '国别/地区（仅经典作品使用，如中国/日本/法国/俄国/英美/拉美等）'
  AFTER `original_author`;
