package com.aitutor.service.impl;

import com.aitutor.service.OnlineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 在线用户追踪：Redis ZSET，member=userId，score=最后活跃时间戳。
 * 活跃超过 ONLINE_TTL 分钟未心跳视为离线。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineServiceImpl implements OnlineService {

    private static final String KEY = "online:users";
    private static final long ONLINE_TTL_MINUTES = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void touch(Long userId) {
        try {
            redisTemplate.opsForZSet().add(KEY, String.valueOf(userId), System.currentTimeMillis());
            redisTemplate.expire(KEY, 1, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("在线状态刷新失败", e);
        }
    }

    @Override
    public void remove(Long userId) {
        try {
            redisTemplate.opsForZSet().remove(KEY, String.valueOf(userId));
        } catch (Exception e) {
            log.warn("在线状态移除失败", e);
        }
    }

    @Override
    public long onlineCount() {
        try {
            cleanupExpired();
            Long size = redisTemplate.opsForZSet().zCard(KEY);
            return size == null ? 0 : size;
        } catch (Exception e) {
            log.warn("在线人数统计失败", e);
            return 0;
        }
    }

    @Override
    public List<Long> onlineUserIds() {
        try {
            cleanupExpired();
            Set<Object> members = redisTemplate.opsForZSet().range(KEY, 0, -1);
            List<Long> ids = new ArrayList<>();
            if (members != null) {
                for (Object m : members) {
                    ids.add(Long.valueOf(String.valueOf(m)));
                }
            }
            return ids;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /** 清理超过 TTL 未活跃的用户 */
    private void cleanupExpired() {
        long cutoff = System.currentTimeMillis() - ONLINE_TTL_MINUTES * 60 * 1000;
        redisTemplate.opsForZSet().removeRangeByScore(KEY, 0, cutoff);
    }
}
