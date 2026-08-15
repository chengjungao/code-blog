package com.site.blog.my.core.util;

import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 IP 的固定窗口限流器（内存版）。
 * 用于保护 AI 助手等昂贵接口，防止被爬虫高频调用。
 */
@Component
public class IpRateLimiter {

    /** 窗口时长（毫秒） */
    private final long windowMillis;
    /** 单个窗口内最大请求次数 */
    private final int maxRequests;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    /** 上次清理时间，避免每次请求都全量扫描 */
    private volatile long lastCleanupTime = System.currentTimeMillis();

    public IpRateLimiter() {
        this(10, 60_000L);
    }

    public IpRateLimiter(int maxRequests, long windowMillis) {
        this.maxRequests = maxRequests;
        this.windowMillis = windowMillis;
    }

    /**
     * 尝试获取一次访问许可。
     *
     * @param key 限流维度标识（通常为 IP）
     * @return true=允许访问, false=超频拒绝
     */
    public boolean tryAcquire(String key) {
        if (key == null || key.isEmpty()) {
            key = "unknown";
        }
        cleanupIfNeeded();

        long now = System.currentTimeMillis();
        // 原子地更新计数
        Window current = windows.get(key);
        if (current == null) {
            Window w = new Window(now, 1);
            Window prev = windows.putIfAbsent(key, w);
            if (prev != null) {
                return acquireFromExisting(prev, now);
            }
            return true;
        }
        return acquireFromExisting(current, now);
    }

    private boolean acquireFromExisting(Window w, long now) {
        synchronized (w) {
            if (now - w.windowStart >= windowMillis) {
                // 进入新窗口，重置计数
                w.windowStart = now;
                w.count = 1;
                return true;
            }
            if (w.count >= maxRequests) {
                return false;
            }
            w.count++;
            return true;
        }
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupTime < windowMillis) {
            return;
        }
        lastCleanupTime = now;
        Iterator<Map.Entry<String, Window>> it = windows.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Window> entry = it.next();
            if (now - entry.getValue().windowStart >= windowMillis) {
                it.remove();
            }
        }
    }

    private static class Window {
        long windowStart;
        int count;

        Window(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
