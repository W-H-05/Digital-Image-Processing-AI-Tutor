package com.aitutor.aspect;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解（基于 Redis 计数）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    /** 限流 key 前缀 */
    String key();
    /** 窗口内最大次数 */
    int limit();
    /** 窗口秒数 */
    int windowSec() default 60;
}
