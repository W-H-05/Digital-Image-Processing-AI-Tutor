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
    /** LibreOffice 可执行文件路径（默认 soffice，需在服务器 PATH 或绝对路径） */
    private String sofficePath = "soffice";
    /** 预览渲染 DPI（默认 120） */
    private int previewDpi = 120;
}
