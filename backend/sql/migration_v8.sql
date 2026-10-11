-- 练习题系统：教师管理选择题（单选/多选），学生即时反馈练习
-- 迁移版本 v8

CREATE TABLE IF NOT EXISTS `practice_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `lesson_pack_id` bigint NOT NULL,
  `question_no` int DEFAULT NULL COMMENT '题号(自动)',
  `stem` text COMMENT '题干',
  `options_json` text COMMENT '选项JSON [{key:"A",text:"..."}]',
  `correct_keys` varchar(32) DEFAULT NULL COMMENT '正确答案，多选用逗号分隔 如 A,C',
  `is_multiple` tinyint DEFAULT '0' COMMENT '是否多选 0单选 1多选',
  `sort_index` int DEFAULT '0',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='练习题表';

CREATE TABLE IF NOT EXISTS `practice_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `lesson_pack_id` bigint DEFAULT NULL,
  `question_id` bigint NOT NULL,
  `is_correct` tinyint DEFAULT '0' COMMENT '是否最终答对',
  `wrong_count` int DEFAULT '0' COMMENT '该题累计错误次数',
  `wrong_reason` text COMMENT '错因反思(选错3次后填写)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='练习作答记录表';
