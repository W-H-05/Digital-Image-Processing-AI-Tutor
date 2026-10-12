-- 练习题题目与选项支持图片：题干图片字段
-- 迁移版本 v9

ALTER TABLE `practice_question`
  ADD COLUMN `stem_image` varchar(512) DEFAULT NULL COMMENT '题干图片路径' AFTER `stem`;
