package com.aitutor;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
@EnableScheduling
public class AiTutorApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(AiTutorApplication.class, args);
    }

    /**
     * 加载 backend/.env 中的环境变量到系统属性（供 application.yml 占位符使用）
     */
    private static void loadDotenv() {
        try {
            String path = ".env";
            if (!Files.exists(Paths.get(path))) {
                path = System.getProperty("user.dir") + "/.env";
            }
            if (Files.exists(Paths.get(path))) {
                Dotenv dotenv = Dotenv.configure().directory(Paths.get(path).toAbsolutePath().getParent().toString())
                        .filename(Paths.get(path).getFileName().toString())
                        .ignoreIfMissing().load();
                dotenv.entries().forEach(e -> {
                    if (System.getProperty(e.getKey()) == null) {
                        System.setProperty(e.getKey(), e.getValue());
                    }
                });
            }
            // 强制设置 server.port（防止环境中 SERVER__PORT 等变量覆盖端口）
            String appPort = System.getProperty("APP_PORT");
            if (appPort != null && !appPort.isBlank()) {
                System.setProperty("server.port", appPort);
            }
        } catch (Exception e) {
            System.err.println("[WARN] .env 加载失败，将使用 application.yml 默认值: " + e.getMessage());
        }
    }
}
