package com.aitutor.service;

/**
 * 在线用户追踪（Redis ZSET 心跳方案）
 */
public interface OnlineService {

    /** 用户活跃时刷新在线状态（登录/操作时调用） */
    void touch(Long userId);

    /** 用户登出时移除 */
    void remove(Long userId);

    /** 当前在线人数（自动清理超时未活跃用户） */
    long onlineCount();

    /** 在线用户 id 列表 */
    java.util.List<Long> onlineUserIds();
}
