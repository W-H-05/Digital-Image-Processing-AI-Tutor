-- ============================================
-- 数字图像处理 AI 助教系统 数据库初始化脚本
-- ============================================
CREATE DATABASE IF NOT EXISTS ai_tutor DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE ai_tutor;

-- 用户表（教师和学生）
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(32) NOT NULL COMMENT '学号/教师账号',
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
  `role` VARCHAR(16) NOT NULL COMMENT 'TEACHER/STUDENT',
  `real_name` VARCHAR(32) DEFAULT NULL,
  `class_name` VARCHAR(64) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `last_login_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 课程表
DROP TABLE IF EXISTS `course`;
CREATE TABLE `course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_name` VARCHAR(128) NOT NULL,
  `description` VARCHAR(512) DEFAULT NULL,
  `teacher_id` BIGINT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程表';

-- 课次包表
DROP TABLE IF EXISTS `lesson_pack`;
CREATE TABLE `lesson_pack` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `course_id` BIGINT NOT NULL,
  `lesson_no` VARCHAR(32) DEFAULT NULL COMMENT '第N次课/实验N',
  `hours` VARCHAR(32) DEFAULT NULL COMMENT '学时',
  `title` VARCHAR(128) NOT NULL,
  `chapter` VARCHAR(128) DEFAULT NULL,
  `objectives` TEXT,
  `content_summary` TEXT COMMENT '内容概述',
  `key_points` TEXT,
  `difficult_points` TEXT,
  `ideological_notes` TEXT,
  `status` VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/OFFLINE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_course` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课次包表';

-- 课次包材料
DROP TABLE IF EXISTS `lesson_material`;
CREATE TABLE `lesson_material` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lesson_pack_id` BIGINT NOT NULL,
  `material_type` VARCHAR(32) DEFAULT NULL,
  `title` VARCHAR(256) DEFAULT NULL,
  `description` TEXT,
  `file_path` VARCHAR(512) DEFAULT NULL,
  `file_type` VARCHAR(64) DEFAULT NULL,
  `file_size` BIGINT DEFAULT 0,
  `sort_index` INT DEFAULT 0,
  `group_name` VARCHAR(32) DEFAULT NULL,
  `is_open_to_student` TINYINT DEFAULT 1,
  `allow_download` TINYINT DEFAULT 1,
  `version` INT DEFAULT 1,
  `status` VARCHAR(16) DEFAULT 'ACTIVE',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课次包材料表';

-- 知识点表
DROP TABLE IF EXISTS `knowledge_point`;
CREATE TABLE `knowledge_point` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lesson_pack_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `description` TEXT,
  `keywords` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点表';

-- 交互课件表
DROP TABLE IF EXISTS `courseware`;
CREATE TABLE `courseware` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lesson_pack_id` BIGINT NOT NULL,
  `name` VARCHAR(128) NOT NULL,
  `type` VARCHAR(32) DEFAULT NULL COMMENT '低通/高通/形态学/分割/特征/视频',
  `config_json` TEXT COMMENT '参数范围/检查点',
  `resource_path` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交互课件表';

-- 学生材料阅览记录
DROP TABLE IF EXISTS `student_material_view`;
CREATE TABLE `student_material_view` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `material_id` BIGINT DEFAULT NULL,
  `view_duration` INT DEFAULT 0 COMMENT '秒',
  `page_no` INT DEFAULT NULL,
  `action_detail` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生材料阅览记录';

-- AI 问答记录
DROP TABLE IF EXISTS `qa_record`;
CREATE TABLE `qa_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `material_id` BIGINT DEFAULT NULL,
  `question` TEXT,
  `answer` MEDIUMTEXT,
  `source_refs` VARCHAR(512) DEFAULT NULL,
  `follow_up_count` INT DEFAULT 0,
  `is_synced_to_teacher` TINYINT DEFAULT 0,
  `has_image` TINYINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答记录';

-- 代码学习辅助记录
DROP TABLE IF EXISTS `code_help_record`;
CREATE TABLE `code_help_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `error_text` TEXT,
  `diagnosis` MEDIUMTEXT,
  `suggestion` MEDIUMTEXT,
  `source_refs` VARCHAR(512) DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码排错记录';

-- 挑战提交
DROP TABLE IF EXISTS `challenge_submission`;
CREATE TABLE `challenge_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `courseware_id` BIGINT DEFAULT NULL,
  `params_json` TEXT,
  `screenshot_path` VARCHAR(512) DEFAULT NULL,
  `conclusion` TEXT,
  `score` INT DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挑战提交表';

-- 学生操作埋点
DROP TABLE IF EXISTS `student_operation`;
CREATE TABLE `student_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `material_id` BIGINT DEFAULT NULL,
  `action_type` VARCHAR(32) DEFAULT NULL,
  `action_detail` VARCHAR(512) DEFAULT NULL,
  `duration` INT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`),
  KEY `idx_pack` (`lesson_pack_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生操作埋点表';

-- 学情统计
DROP TABLE IF EXISTS `learning_stats`;
CREATE TABLE `learning_stats` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `knowledge_point_id` BIGINT DEFAULT NULL,
  `mastery_score` DECIMAL(5,2) DEFAULT 0,
  `confusion_count` INT DEFAULT 0,
  `error_count` INT DEFAULT 0,
  `participation_score` DECIMAL(5,2) DEFAULT 0,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学情统计表';

-- 困惑点聚类
DROP TABLE IF EXISTS `confusion_cluster`;
CREATE TABLE `confusion_cluster` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `knowledge_point_id` BIGINT DEFAULT NULL,
  `cluster_name` VARCHAR(128) DEFAULT NULL,
  `question_count` INT DEFAULT 0,
  `student_count` INT DEFAULT 0,
  `sample_questions` TEXT,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='困惑点聚类表';

-- 互评记录
DROP TABLE IF EXISTS `peer_review`;
CREATE TABLE `peer_review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reviewer_id` BIGINT NOT NULL,
  `reviewee_id` BIGINT NOT NULL,
  `task_id` BIGINT DEFAULT NULL,
  `scores_json` TEXT,
  `comments` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_reviewer` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互评记录表';

-- 教师备课笔记
DROP TABLE IF EXISTS `teacher_note`;
CREATE TABLE `teacher_note` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `teacher_id` BIGINT NOT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `note_content` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师备课笔记';

-- 系统配置
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(64) NOT NULL,
  `config_value` VARCHAR(1024) DEFAULT NULL,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- RAG 知识库文档
DROP TABLE IF EXISTS `rag_document`;
CREATE TABLE `rag_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `title` VARCHAR(256) DEFAULT NULL,
  `category` VARCHAR(32) DEFAULT NULL COMMENT '教材/教案/OpenCV/NumPy/报错库',
  `content` MEDIUMTEXT,
  `chunk_index` INT DEFAULT 0,
  `embedding` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG知识库文档';

-- 材料版本历史表
DROP TABLE IF EXISTS `lesson_material_version`;
CREATE TABLE `lesson_material_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `material_id` BIGINT NOT NULL,
  `version` INT DEFAULT 1,
  `title` VARCHAR(256) DEFAULT NULL,
  `description` TEXT,
  `file_path` VARCHAR(512) DEFAULT NULL,
  `file_type` VARCHAR(64) DEFAULT NULL,
  `file_size` BIGINT DEFAULT 0,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_material` (`material_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='材料版本历史表';

-- Prompt 模板表
DROP TABLE IF EXISTS `prompt_template`;
CREATE TABLE `prompt_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `purpose` VARCHAR(32) DEFAULT NULL COMMENT 'chat/code/func/prepare/vision',
  `content` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt模板表';

-- 互评量表表
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

-- 互评活动表
DROP TABLE IF EXISTS `peer_review_activity`;
CREATE TABLE `peer_review_activity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `mode` VARCHAR(32) DEFAULT NULL COMMENT 'gallery/debate/jigsaw',
  `rubric_id` BIGINT DEFAULT NULL,
  `lesson_pack_id` BIGINT DEFAULT NULL,
  `duration_sec` INT DEFAULT 300 COMMENT '计时秒数',
  `status` VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/FINISHED',
  `start_time` DATETIME DEFAULT NULL,
  `end_time` DATETIME DEFAULT NULL,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='互评活动表';

-- 作业表
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

-- 作业提交表
DROP TABLE IF EXISTS `homework_submission`;
CREATE TABLE `homework_submission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `homework_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `answers_json` TEXT COMMENT '学生答案JSON',
  `score` INT DEFAULT NULL,
  `auto_scored` TINYINT DEFAULT 1,
  `submit_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_homework` (`homework_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业提交表';
