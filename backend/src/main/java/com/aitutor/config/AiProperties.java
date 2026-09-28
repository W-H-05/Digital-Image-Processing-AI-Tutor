package com.aitutor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置属性（读取 application.yml 中 ai.* 配置，值来自 .env）
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
    private String visionModel;
}
