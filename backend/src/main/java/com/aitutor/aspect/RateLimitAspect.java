package com.aitutor.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.aitutor.common.BizException;
import com.aitutor.config.RuntimeConfigService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 限流切面：Redis INCR + 过期时间实现滑动计数。
 * 阈值优先读取运行时配置（ratelimit.{key}.limit / ratelimit.{key}.windowSec），
 * 未配置时使用注解默认值，从而支持前端动态配置调用次数与成本。
 */
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RuntimeConfigService configService;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        String userId = "anonymous";
        try {
            userId = String.valueOf(StpUtil.getLoginIdAsLong());
        } catch (Exception ignored) {
        }
        // 动态读取限流阈值
        int limit = configService.getInt("ratelimit." + rateLimit.key() + ".limit", rateLimit.limit());
        int windowSec = configService.getInt("ratelimit." + rateLimit.key() + ".windowSec", rateLimit.windowSec());

        String redisKey = "ratelimit:" + rateLimit.key() + ":" + userId;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, windowSec, TimeUnit.SECONDS);
        }
        if (count != null && count > limit) {
            throw new BizException(429, "操作过于频繁，请稍后再试（每 " + windowSec + " 秒限 " + limit + " 次）");
        }
        return pjp.proceed();
    }
}
