package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.PageStatMapper;
import com.site.blog.my.core.entity.PageStat;
import com.site.blog.my.core.service.VisitStatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 页面访问统计实现。
 * 实时访问数据只进内存（PV 用 LongAdder、UV 用 IP 去重集合），
 * 由定时任务每 5 分钟聚合一次写入 DB（Summary 数据）。
 */
@Service
public class VisitStatServiceImpl implements VisitStatService {

    private static final String SEPARATOR = "|";

    // PV 计数器：key = yyyy-MM-dd|pagePath -> LongAdder（聚合时清零）
    private final Map<String, LongAdder> pvCounter = new ConcurrentHashMap<>();
    // UV 计数器：key = yyyy-MM-dd|pagePath -> 去重 IP 集合（保留全天，跨聚合周期不重复计数）
    private final Map<String, Set<String>> uvCounter = new ConcurrentHashMap<>();

    @Autowired
    private PageStatMapper pageStatMapper;

    @Override
    public void record(String pagePath, String ip) {
        if (pagePath == null || pagePath.isEmpty()) {
            return;
        }
        String key = LocalDate.now() + SEPARATOR + pagePath;
        pvCounter.computeIfAbsent(key, k -> new LongAdder()).increment();
        uvCounter.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(ip == null || ip.isEmpty() ? "unknown" : ip);
    }

    @Override
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void flush() {
        flushPv();
        flushUv();
        cleanExpired();
    }

    /** PV：原子清零并累加写入 DB */
    private void flushPv() {
        List<PageStat> toSave = new ArrayList<>();
        for (Map.Entry<String, LongAdder> e : pvCounter.entrySet()) {
            long v = e.getValue().sumThenReset();
            if (v <= 0) {
                continue;
            }
            String[] parts = splitKey(e.getKey());
            PageStat stat = new PageStat();
            stat.setStatDate(parts[0]);
            stat.setPagePath(parts[1]);
            stat.setPv((int) v);
            toSave.add(stat);
        }
        for (PageStat stat : toSave) {
            try {
                pageStatMapper.upsertPv(stat);
            } catch (Exception ignored) {
                // 单条失败不影响其他
            }
        }
    }

    /** UV：不清空集合，用当前 size 覆盖写入 DB（保证跨周期不重复） */
    private void flushUv() {
        for (Map.Entry<String, Set<String>> e : uvCounter.entrySet()) {
            int uv = e.getValue().size();
            if (uv <= 0) {
                continue;
            }
            String[] parts = splitKey(e.getKey());
            PageStat stat = new PageStat();
            stat.setStatDate(parts[0]);
            stat.setPagePath(parts[1]);
            stat.setUv(uv);
            try {
                pageStatMapper.upsertUv(stat);
            } catch (Exception ignored) {
            }
        }
    }

    /** 清理昨天及更早的内存 key，避免内存泄漏 */
    private void cleanExpired() {
        String today = LocalDate.now().toString();
        pvCounter.keySet().removeIf(key -> key.substring(0, 10).compareTo(today) < 0);
        uvCounter.keySet().removeIf(key -> key.substring(0, 10).compareTo(today) < 0);
    }

    private String[] splitKey(String key) {
        int idx = key.indexOf(SEPARATOR);
        return new String[]{key.substring(0, idx), key.substring(idx + 1)};
    }

    @Override
    public Map<String, Object> getStatOverview(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> daily = pageStatMapper.selectDailySummary(startDate, endDate);
        List<Map<String, Object>> topPages = pageStatMapper.selectTopPages(startDate, endDate, 10);
        Map<String, Object> total = pageStatMapper.selectTotalSummary(startDate, endDate);
        result.put("daily", daily == null ? Collections.emptyList() : daily);
        result.put("topPages", topPages == null ? Collections.emptyList() : topPages);
        result.put("total", total == null ? Collections.emptyMap() : total);
        return result;
    }
}
