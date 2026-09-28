package com.aitutor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用配置（上传目录、教师账号等）
 */
@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private String uploadDir;
    private String teacherUsername;
    private String teacherPassword;
}
