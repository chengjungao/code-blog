package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.PageStat;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * 当日访问内存计数（PV 计数 + UV 去重集合）。
 *
 * <p>刻意做成<b>零依赖的独立 bean</b>，原因是依赖方向：
 * <pre>
 *   写盘消费者 ──记录──▶ PageStatCounters ◀──读取── 定时落库 / 报表
 * </pre>
 * 如果让写盘服务直接依赖 {@code VisitStatService}，就会形成
 * 「写盘 → 统计 → 写盘」的循环依赖（Spring Boot 2.6+ 默认禁止），
 * 拆出这个 bean 后依赖图是单向的，也就不需要 {@code @Lazy} 之类的绕法。
 *
 * <p>并发模型：写方是单消费者线程，读方是定时任务/报表线程。UV 用
 * {@code ConcurrentHashMap.newKeySet()}；PV 用 {@link LongAdder}，
 * 落库时 {@code sumThenReset()} 原子清零，保证「同一 PV 不会被算两次」。
 */
@Component
public class PageStatCounters {

    private static final String SEP = "|";

    private final Map<String, LongAdder> pv = new ConcurrentHashMap<String, LongAdder>();
    private final Map<String, Set<String>> uv = new ConcurrentHashMap<String, Set<String>>();

    /** 记录一次有效访问（调用方需已过滤爬虫与内网流量） */
    public void record(String pagePath, String ip) {
        if (pagePath == null || pagePath.isEmpty()) {
            return;
        }
        String key = LocalDate.now() + SEP + pagePath;
        LongAdder adder = pv.get(key);
        if (adder == null) {
            adder = pv.computeIfAbsent(key, k -> new LongAdder());
        }
        adder.increment();
        Set<String> set = uv.get(key);
        if (set == null) {
            set = uv.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
        }
        set.add(ip == null || ip.isEmpty() ? "unknown" : ip);
    }

    /** PV 增量：原子取走并清零，用于累加写 DB */
    public List<PageStat> drainPv() {
        List<PageStat> list = new ArrayList<PageStat>();
        for (Map.Entry<String, LongAdder> e : pv.entrySet()) {
            long v = e.getValue().sumThenReset();
            if (v <= 0) {
                continue;
            }
            list.add(build(e.getKey(), (int) v, null));
        }
        return list;
    }

    /**
     * UV 快照：<b>不清空</b>集合（集合留到跨天才清），用当前 size 覆盖写 DB，
     * 这样跨 5 分钟聚合周期不会重复计数。
     */
    public List<PageStat> snapshotUv() {
        List<PageStat> list = new ArrayList<PageStat>();
        for (Map.Entry<String, Set<String>> e : uv.entrySet()) {
            int v = e.getValue().size();
            if (v <= 0) {
                continue;
            }
            list.add(build(e.getKey(), null, v));
        }
        return list;
    }

    /** 清理非今日的 key，避免长跑进程内存泄漏 */
    public void cleanBefore(LocalDate today) {
        final String t = today.toString();
        pv.keySet().removeIf(k -> k.length() > 10 && k.substring(0, 10).compareTo(t) < 0);
        uv.keySet().removeIf(k -> k.length() > 10 && k.substring(0, 10).compareTo(t) < 0);
    }

    /** 当日实时概览（报表页「今日实时」用，不等 5 分钟聚合） */
    public Map<String, Object> todaySnapshot() {
        String today = LocalDate.now().toString();
        long totalPv = 0;
        int paths = 0;
        Set<String> uniq = ConcurrentHashMap.newKeySet();
        for (Map.Entry<String, LongAdder> e : pv.entrySet()) {
            if (!e.getKey().startsWith(today + SEP)) {
                continue;
            }
            long v = e.getValue().sum();
            if (v > 0) {
                totalPv += v;
                paths++;
            }
        }
        for (Map.Entry<String, Set<String>> e : uv.entrySet()) {
            if (e.getKey().startsWith(today + SEP)) {
                uniq.addAll(e.getValue());
            }
        }
        Map<String, Object> m = new java.util.LinkedHashMap<String, Object>();
        m.put("date", today);
        m.put("pv", totalPv);
        m.put("uv", uniq.size());
        m.put("paths", paths);
        return m;
    }

    private PageStat build(String key, Integer pvValue, Integer uvValue) {
        int idx = key.indexOf(SEP);
        PageStat s = new PageStat();
        s.setStatDate(key.substring(0, idx));
        s.setPagePath(key.substring(idx + 1));
        s.setPv(pvValue == null ? 0 : pvValue);
        s.setUv(uvValue == null ? 0 : uvValue);
        s.setCreateTime(Calendar.getInstance().getTime());
        return s;
    }
}
