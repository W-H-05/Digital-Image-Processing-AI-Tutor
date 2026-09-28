USE ai_tutor;

-- 1. 互评量表表（教师配置评分规则）
DROP TABLE IF EXISTS `peer_review_rubric`;
CREATE TABLE `peer_review_rubric` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `dimensions_json` TEXT COMMENT '评分维度JSON [{name,desc,weight}]',
  `description` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互评量表表';

-- 2. 互评活动表（课堂互评模式：画廊漫步/微型辩论/专家拼图，含计时）
DROP TABLE IF EXISTS `peer_review_activity`;
CREATE TABLE `peer_review_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `mode` VARCHAR(32) DEFAULT NULL COMMENT 'gallery/debate/jigsaw 画廊漫步/微型辩论/专家拼图',
  `rubric_id` BIGINT DEFAULT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `duration_sec` INT DEFAULT 300 COMMENT '计时秒数',
  `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/FINISHED',
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互评活动表';

-- 3. 作业表（课次包作业，含题目 JSON）
DROP TABLE IF EXISTS `homework`;
CREATE TABLE `homework` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lesson_pack_id` BIGINT NOT NULL,
  `title` VARCHAR(256) NOT NULL,
  `description` TEXT,
  `questions_json` MEDIUMTEXT COMMENT '题目JSON [{type,stem,options[],answer,score}]',
  `deadline` DATETIME DEFAULT NULL,
  `total_score` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- 4. 作业提交表（学生提交与自动判分）
DROP TABLE IF EXISTS `homework_submission`;
CREATE TABLE `homework_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `homework_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `answers_json` TEXT COMMENT '学生答案JSON',
  `score` INT DEFAULT NULL,
  `auto_scored` TINYINT DEFAULT 1 COMMENT '是否自动判分',
  `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_homework` (`homework_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';

-- 5. 初始化默认互评量表
INSERT INTO `peer_review_rubric` (`name`, `dimensions_json`, `description`) VALUES
('默认互评量表', '[{"name":"参数合理性","desc":"参数设置是否符合图像处理原理","weight":40},{"name":"结论正确性","desc":"实验结论是否正确","weight":40},{"name":"展示清晰度","desc":"截图与说明是否清晰","weight":20}]', '三维度默认量表：参数合理性40% + 结论正确性40% + 展示清晰度20%');
