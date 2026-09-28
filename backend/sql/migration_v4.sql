USE ai_tutor;

-- 初始化系统配置默认值（AI 参数、限流阈值、备份配置）
INSERT INTO `system_config` (`config_key`, `config_value`) VALUES
('ai.baseUrl', 'https://api.deepseek.com'),
('ai.apiKey', 'sk-your-deepseek-api-key-here'),
('ai.model', 'deepseek-chat'),
('ai.visionModel', 'deepseek-vl'),
('ratelimit.ai-chat.limit', '30'),
('ratelimit.ai-chat.windowSec', '3600'),
('ratelimit.ai-image.limit', '10'),
('ratelimit.ai-image.windowSec', '3600'),
('ratelimit.ai-code.limit', '20'),
('ratelimit.ai-code.windowSec', '3600'),
('ratelimit.ai-func.limit', '30'),
('ratelimit.ai-func.windowSec', '3600'),
('ratelimit.ai-prepare.limit', '20'),
('ratelimit.ai-prepare.windowSec', '3600'),
('backup.enabled', 'true'),
('backup.path', './backup'),
('backup.keepCount', '10');
