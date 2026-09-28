package com.aitutor.config;

import com.aitutor.entity.SystemConfig;
import com.aitutor.mapper.SystemConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时配置服务：从 system_config 表读取配置，内存缓存，修改后 reload 即可动态生效。
 * 用于 AI 参数、限流阈值、备份配置等需要前端可配置的项。
 * 密钥通过 .env 提供默认值，运行时可在前端覆盖，不硬编码在代码中。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuntimeConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        reload();
    }

    /** 从数据库重载全部配置 */
    public synchronized void reload() {
        try {
            List<SystemConfig> list = systemConfigMapper.selectList(null);
            cache.clear();
            for (SystemConfig c : list) {
                cache.put(c.getConfigKey(), c.getConfigValue());
            }
            log.info("运行时配置已加载，共 {} 项", cache.size());
        } catch (Exception e) {
            log.warn("运行时配置加载失败（可能表未初始化）", e);
        }
    }

    /** 读取字符串配置，不存在返回默认值 */
    public String get(String key, String defaultValue) {
        String v = cache.get(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }

    /** 读取整数配置 */
    public int getInt(String key, int defaultValue) {
        String v = cache.get(key);
        try {
            return (v == null || v.isBlank()) ? defaultValue : Integer.parseInt(v.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** 读取布尔配置 */
    public boolean getBool(String key, boolean defaultValue) {
        String v = cache.get(key);
        if (v == null || v.isBlank()) return defaultValue;
        return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
    }

    /** 读取全部配置（脱敏） */
    public Map<String, String> all() {
        Map<String, String> result = new ConcurrentHashMap<>(cache);
        // 密钥类配置脱敏
        result.replaceAll((k, v) -> {
            if (k != null && k.toLowerCase().contains("key") && v != null && v.length() > 8) {
                return v.substring(0, 4) + "****" + v.substring(v.length() - 4);
            }
            return v;
        });
        return result;
    }
}
