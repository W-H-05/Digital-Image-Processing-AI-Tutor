USE ai_tutor;

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

INSERT INTO `prompt_template` (`name`, `purpose`, `content`) VALUES
('RAG问答', 'chat', '你是《数字图像处理》课程的AI助教「贾维斯」。基于知识库片段回答，回答要准确通俗、面向初学者，标注来源。'),
('代码排错', 'code', '你是代码排错助手，解释报错原因、给出修复方向，不代替学生写完整代码、不执行代码。'),
('函数查询', 'func', '你是OpenCV/NumPy函数查询助手，给出签名、参数、返回值、示例、注意点。'),
('备课助手', 'prepare', '你是备课助手，从教案抽取教学目标、重难点、思政、讨论题、挑战任务，以JSON返回。'),
('图片分析', 'vision', '你是数字图像处理课程AI助教，客观描述图像内容，结合图像处理知识分析，给出学习建议。');
