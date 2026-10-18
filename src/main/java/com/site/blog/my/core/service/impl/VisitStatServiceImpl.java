package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.PageStatMapper;
import com.site.blog.my.core.entity.PageStat;
import com.site.blog.my.core.entity.PageStatEvent;
import com.site.blog.my.core.service.PageStatCounters;
import com.site.blog.my.core.service.PageStatLogService;
import com.site.blog.my.core.service.VisitStatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 页面访问统计。
 *
 * <p>两层数据：
 * <ol>
 *   <li><b>明细层</b>（{@link PageStatLogService} 写 JSONL）：保留 IP/来源/设备/停留等维度，
 *       是「报表能多维度切」的基础。</li>
 *   <li><b>汇总层</b>（{@link PageStatCounters} → tb_page_stat，5 分钟一次）：
 *       沿用既有表结构与查询，保证历史趋势不断档。</li>
 * </ol>
 *
 * <p>报表聚合以明细为准、DB 兜底：明细文件只从功能上线那天起才有，更早的日期回退到 DB 汇总，
 * 并在趋势点上标注 {@code src=db}，让「哪几天是粗略口径」对使用者可见，而不是悄无声息地混在一起。
 */
@Service
public class VisitStatServiceImpl implements VisitStatService {

    private static final Logger LOG = LoggerFactory.getLogger(VisitStatServiceImpl.class);

    /** 报表单次最多回溯天数、最多处理事件数（防止一次查询把 JVM 拖垮） */
    private static final int MAX_RANGE_DAYS = 90;
    private static final int MAX_EVENTS = 2000000;
    /** 报表结果短缓存：避免 admin 页反复刷新时反复解析明细文件 */
    private static final long REPORT_TTL_MS = 30000L;
    /** 单个维度集合最多容纳的身份数，防止长区间把内存吃满 */
    private static final int MAX_KEYS = 200000;

    @Autowired
    private PageStatMapper pageStatMapper;

    @Autowired
    private PageStatLogService pageStatLogService;

    @Autowired
    private PageStatCounters counters;

    /** 本站域名，用于把来源区分成「站内跳转」与「外部链接」 */
    @Value("${blog.base-url:}")
    private String baseUrl;

    private final Map<String, Object[]> reportCache = new ConcurrentHashMap<String, Object[]>();

    // ==================== 写入 / 落库 ====================

    @Override
    public void record(String pagePath, String ip) {
        counters.record(pagePath, ip);
    }

    @Override
    @Scheduled(fixedRate = 300000, initialDelay = 60000)
    public void flush() {
        for (PageStat stat : counters.drainPv()) {
            try {
                pageStatMapper.upsertPv(stat);
            } catch (Exception e) {
                LOG.warn("[stat] PV 落库失败 {}：{}", stat.getPagePath(), e.toString());
            }
        }
        for (PageStat stat : counters.snapshotUv()) {
            try {
                pageStatMapper.upsertUv(stat);
            } catch (Exception e) {
                LOG.warn("[stat] UV 落库失败 {}：{}", stat.getPagePath(), e.toString());
            }
        }
        counters.cleanBefore(LocalDate.now());
    }

    @Override
    public Map<String, Object> getRealtime() {
        return counters.todaySnapshot();
    }

    @Override
    public Map<String, Object> getStatOverview(String startDate, String endDate) {
        Map<String, Object> result = new HashMap<String, Object>();
        List<Map<String, Object>> daily = pageStatMapper.selectDailySummary(startDate, endDate);
        List<Map<String, Object>> topPages = pageStatMapper.selectTopPages(startDate, endDate, 10);
        Map<String, Object> total = pageStatMapper.selectTotalSummary(startDate, endDate);
        result.put("daily", daily == null ? Collections.emptyList() : daily);
        result.put("topPages", topPages == null ? Collections.emptyList() : topPages);
        result.put("total", total == null ? Collections.emptyMap() : total);
        return result;
    }

    // ==================== 多维报表 ====================

    @Override
    public Map<String, Object> getStatReport(String startDate, String endDate) {
        LocalDate today = LocalDate.now();
        LocalDate to = parseDate(endDate, today);
        LocalDate from = parseDate(startDate, today.minusDays(13));
        if (from.isAfter(to)) {
            LocalDate t = from;
            from = to;
            to = t;
        }
        if (from.isBefore(to.minusDays(MAX_RANGE_DAYS - 1L))) {
            from = to.minusDays(MAX_RANGE_DAYS - 1L);
        }

        String key = from + "~" + to;
        Object[] hit = reportCache.get(key);
        if (hit != null && System.currentTimeMillis() - (Long) hit[0] < REPORT_TTL_MS) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cached = (Map<String, Object>) hit[1];
            return cached;
        }

        Agg agg = new Agg(from, to, baseUrl);
        try {
            pageStatLogService.forEachEvent(from, to, agg);
        } catch (Exception e) {
            LOG.warn("[stat] 读取访问明细失败：{}", e.toString());
        }
        Map<String, Object> report = agg.build();
        applyDbFallback(report, from, to);
        report.put("realtime", counters.todaySnapshot());
        report.put("queue", pageStatLogService.health());
        report.put("files", pageStatLogService.files());
        report.put("generatedAt", java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        if (reportCache.size() > 8) {
            reportCache.clear();
        }
        reportCache.put(key, new Object[]{System.currentTimeMillis(), report});
        return report;
    }

    /**
     * 明细缺失的日期回退到 DB 汇总。
     *
     * <p>只有「明细没有这天数据」时才回退，避免把明细与 DB 两套口径混在一张图里。
     * 回退点会被标成 {@code src=db}，前端据此把「粗略口径」与「明细口径」区分显示。
     */
    private void applyDbFallback(Map<String, Object> report, LocalDate from, LocalDate to) {
        Map<String, Map<String, Object>> dbRows = new HashMap<String, Map<String, Object>>();
        try {
            List<Map<String, Object>> rows = pageStatMapper.selectDailySummary(from.toString(), to.toString());
            if (rows != null) {
                for (Map<String, Object> r : rows) {
                    String d = asDate(r.get("statdate"));
                    if (d != null) {
                        dbRows.put(d, r);
                    }
                }
            }
        } catch (Exception e) {
            LOG.warn("[stat] DB 汇总回退失败：{}", e.toString());
        }

        List<Map<String, Object>> trend = castList(report.get("trend"));
        long dbPv = 0;
        long dbUv = 0;
        int dbDays = 0;
        for (Map<String, Object> t : trend) {
            if (!"none".equals(t.get("src"))) {
                continue;
            }
            Map<String, Object> r = dbRows.get(String.valueOf(t.get("date")));
            if (r == null) {
                continue;
            }
            long pv = asLong(r.get("pv"));
            long uv = asLong(r.get("uv"));
            t.put("pv", pv);
            t.put("uv", uv);
            t.put("sessions", 0);
            t.put("avgDuration", 0);
            t.put("src", "db");
            dbPv += pv;
            dbUv += uv;
            dbDays++;
        }
        // 同步旧字段，保证老调用方（只认 daily/topPages/total）拿到的是同一份数
        List<Map<String, Object>> daily = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> t : trend) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("statdate", t.get("date"));
            m.put("pv", t.get("pv"));
            m.put("uv", t.get("uv"));
            daily.add(m);
        }
        report.put("daily", daily);

        @SuppressWarnings("unchecked")
        Map<String, Object> summary = (Map<String, Object>) report.get("summary");
        @SuppressWarnings("unchecked")
        Map<String, Object> total = (Map<String, Object>) report.get("total");
        // PV 可以直接相加：回退的都是「完全没有明细」的天，不存在重复计数
        long logPv = asLong(summary.get("pv"));
        long mergedPv = logPv + dbPv;
        // 报表卡片读的是 summary.pv（不是 total.pv）⇒ 合并结果必须写回 summary，
        // 否则「明细上线前」那几天的 PV 只落在 total 里，卡片上永远看不到（实测少算 95%）
        summary.put("pvLog", logPv);
        summary.put("pv", mergedPv);
        total.put("pv", mergedPv);
        // UV 不能相加——两套源的去重集合不同，相加必然虚高；总量只报明细口径，
        // 回退天数的粗略 UV 已经在趋势点里按 src=db 单独标出来了
        total.put("uv", summary.get("uv"));

        @SuppressWarnings("unchecked")
        Map<String, Object> health = (Map<String, Object>) report.get("health");
        health.put("dbFallbackDays", dbDays);
        health.put("dbFallbackPv", dbPv);
    }

    private static String asDate(Object o) {
        if (o == null) {
            return null;
        }
        String s = String.valueOf(o).trim();
        return s.length() >= 10 ? s.substring(0, 10) : null;
    }

    private static long asLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o));
        } catch (Exception e) {
            return 0L;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object o) {
        return o instanceof List ? (List<Map<String, Object>>) o : new ArrayList<Map<String, Object>>();
    }

    private static LocalDate parseDate(String s, LocalDate fallback) {
        if (s == null || s.trim().isEmpty()) {
            return fallback;
        }
        try {
            return LocalDate.parse(s.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    // ==================== 单遍聚合器 ====================

    private static final class DayAgg {
        long pv;
        long durSum;
        long durCnt;
        final Set<String> ips = new HashSet<String>();
        final Set<String> sids = new HashSet<String>();
    }

    private static final class PathAgg {
        long pv;
        long entries;
        long durSum;
        long durCnt;
        final Set<String> ips = new HashSet<String>();
    }

    /**
     * 一次遍历算完全部维度，避免为了 8 张图读 8 遍明细文件。
     * 非 Spring bean，纯计算，便于单测。
     */
    private static final class Agg implements Consumer<PageStatEvent> {

        private static final String[][] DUR_BUCKETS = {
                {"0-10 秒", "0", "10"}, {"10-30 秒", "10", "30"}, {"30-60 秒", "30", "60"},
                {"1-3 分钟", "60", "180"}, {"3-10 分钟", "180", "600"}, {"10 分钟以上", "600", "-1"}};

        private static final String[] SEARCH_HOSTS = {
                "baidu.", "google.", "bing.", "sogou.", "so.com", "sm.cn", "toutiao.",
                "duckduckgo.", "yandex.", "yahoo.", "ecosia.", "naver.", "qwant.",
                "brave.com", "startpage.", "search."};

        private final LocalDate from;
        private final LocalDate to;
        /** 本站域名，必须在遍历前确定，否则来源分类会把站内跳转误判成外部 */
        private final String ownHost;

        private final Map<String, DayAgg> days = new TreeMap<String, DayAgg>();
        private final Map<String, PathAgg> paths = new HashMap<String, PathAgg>();
        private final long[] hourlyPv = new long[24];
        private final Map<Integer, Set<String>> hourlyUv = new HashMap<Integer, Set<String>>();
        private final Map<String, Long> sourcePv = new HashMap<String, Long>();
        private final Map<String, Long> sourceHostPv = new HashMap<String, Long>();
        private final Map<String, Long> devices = new HashMap<String, Long>();
        private final Map<String, Long> browsers = new HashMap<String, Long>();
        private final Map<String, Long> systems = new HashMap<String, Long>();
        private final Map<String, Long> durBuckets = new LinkedHashMap<String, Long>();
        private final Map<String, Set<String>> vidDays = new HashMap<String, Set<String>>();
        private final Map<String, Integer> sidPages = new HashMap<String, Integer>();
        private final Set<String> allIps = new HashSet<String>();

        private long events;
        private long pvEvents;
        private long leaveEvents;
        private long botEvents;
        private long internalEvents;
        private long entryEvents;
        private long durSum;
        private long durCount;
        private boolean truncated;

        Agg(LocalDate from, LocalDate to, String baseUrl) {
            this.from = from;
            this.to = to;
            this.ownHost = hostOfUrl(baseUrl);
            for (String[] b : DUR_BUCKETS) {
                durBuckets.put(b[0], 0L);
            }
        }

        @Override
        public void accept(PageStatEvent e) {
            if (e == null) {
                return;
            }
            events++;
            if (events > MAX_EVENTS) {
                truncated = true;
                return;
            }
            // 爬虫 / 内网流量：明细文件里保留（可核对），但报表口径剔除
            if (e.isBot()) {
                botEvents++;
                return;
            }
            if (e.isInternal()) {
                internalEvents++;
                return;
            }
            String day = dayOf(e);
            if (day == null) {
                return;
            }
            String ip = nz(e.getIp());
            String path = e.getPath() == null ? "/" : e.getPath();

            if ("leave".equals(e.getType())) {
                leaveEvents++;
                Long d = e.getDur();
                if (d != null && d >= 0 && d <= 86400) {
                    durSum += d;
                    durCount++;
                    DayAgg da = days.get(day);
                    if (da != null) {
                        da.durSum += d;
                        da.durCnt++;
                    }
                    PathAgg pa = paths.get(path);
                    if (pa != null) {
                        pa.durSum += d;
                        pa.durCnt++;
                    }
                    String label = bucketOf(d);
                    durBuckets.put(label, durBuckets.get(label) + 1);
                }
                return;
            }

            // ---- PV ----
            pvEvents++;
            allIps.add(ip);
            DayAgg da = days.get(day);
            if (da == null) {
                da = new DayAgg();
                days.put(day, da);
            }
            da.pv++;
            if (da.ips.size() < MAX_KEYS) {
                da.ips.add(ip);
            }
            String sid = nz(e.getSid());
            if (!sid.isEmpty() && da.sids.size() < MAX_KEYS) {
                da.sids.add(sid);
                Integer c = sidPages.get(sid);
                sidPages.put(sid, c == null ? 1 : c + 1);
            }

            int hour = hourOf(e.getSt());
            if (hour >= 0) {
                hourlyPv[hour]++;
                Set<String> hu = hourlyUv.get(hour);
                if (hu == null) {
                    hu = new HashSet<String>();
                    hourlyUv.put(hour, hu);
                }
                if (hu.size() < MAX_KEYS) {
                    hu.add(ip);
                }
            }

            PathAgg pa = paths.get(path);
            if (pa == null) {
                pa = new PathAgg();
                paths.put(path, pa);
            }
            pa.pv++;
            if (pa.ips.size() < MAX_KEYS) {
                pa.ips.add(ip);
            }

            bump(devices, nz(e.getDevice(), "未知"));
            bump(browsers, nz(e.getBrowser(), "未知"));
            bump(systems, nz(e.getOs(), "未知"));

            String vid = nz(e.getVid());
            if (!vid.isEmpty() && vidDays.size() < MAX_KEYS) {
                Set<String> ds = vidDays.get(vid);
                if (ds == null) {
                    ds = new HashSet<String>();
                    vidDays.put(vid, ds);
                }
                ds.add(day);
            }

            // 来源只在落地页计入，避免站内跳转被误算成外部流量
            if (e.isEntry()) {
                entryEvents++;
                pa.entries++;
                String ref = nz(e.getRef());
                String type = ref.isEmpty() ? "直接访问" : classify(ref);
                bump(sourcePv, type);
                if ("外部链接".equals(type) || "搜索引擎".equals(type)) {
                    bump(sourceHostPv, hostOfRef(ref));
                }
            }
        }

        private String classify(String ref) {
            String host = hostOfRef(ref).toLowerCase();
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            if (!ownHost.isEmpty() && (host.equals(ownHost) || host.endsWith("." + ownHost))) {
                return "站内跳转";
            }
            for (String s : SEARCH_HOSTS) {
                if (host.contains(s)) {
                    return "搜索引擎";
                }
            }
            return "外部链接";
        }

        // ---------- 输出 ----------

        Map<String, Object> build() {
            Map<String, Object> out = new LinkedHashMap<String, Object>();

            Map<String, Object> range = new LinkedHashMap<String, Object>();
            range.put("start", from.toString());
            range.put("end", to.toString());
            range.put("days", (int) (to.toEpochDay() - from.toEpochDay() + 1));
            out.put("range", range);

            int sessions = sidPages.size();
            int bounces = 0;
            for (Integer c : sidPages.values()) {
                if (c != null && c == 1) {
                    bounces++;
                }
            }
            int newV = 0;
            int retV = 0;
            for (Set<String> ds : vidDays.values()) {
                if (ds.size() <= 1) {
                    newV++;
                } else {
                    retV++;
                }
            }

            Map<String, Object> summary = new LinkedHashMap<String, Object>();
            summary.put("pv", pvEvents);
            summary.put("uv", allIps.size());
            summary.put("sessions", sessions);
            summary.put("entries", entryEvents);
            summary.put("avgDuration", durCount <= 0 ? 0 : Math.round((double) durSum / durCount));
            summary.put("durationSamples", durCount);
            summary.put("bounceRate", sessions <= 0 ? 0
                    : Math.round(bounces * 1000.0 / sessions) / 10.0);
            summary.put("pagesPerSession", sessions <= 0 ? 0
                    : Math.round(pvEvents * 100.0 / sessions) / 100.0);
            summary.put("newVisitors", newV);
            summary.put("returningVisitors", retV);
            out.put("summary", summary);

            out.put("trend", trend());
            out.put("hourly", hourly());
            out.put("pages", pages());
            out.put("sources", distributions(sourcePv, false));
            out.put("sourceHosts", distributions(sourceHostPv, true));
            out.put("devices", distributions(devices, false));
            out.put("browsers", distributions(browsers, true));
            out.put("systems", distributions(systems, true));
            out.put("durations", durationBuckets());
            out.put("health", health());

            // 兼容旧字段
            List<Map<String, Object>> daily = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> t : trend()) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("statdate", t.get("date"));
                m.put("pv", t.get("pv"));
                m.put("uv", t.get("uv"));
                daily.add(m);
            }
            out.put("daily", daily);
            List<Map<String, Object>> top = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> p : pages()) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("pagepath", p.get("path"));
                m.put("pv", p.get("pv"));
                m.put("uv", p.get("uv"));
                top.add(m);
            }
            out.put("topPages", top);
            Map<String, Object> total = new LinkedHashMap<String, Object>();
            total.put("pv", pvEvents);
            total.put("uv", allIps.size());
            out.put("total", total);
            return out;
        }

        /** 趋势：明细优先；缺失日期留 src=none，由调用方按需回退 DB */
        private List<Map<String, Object>> trend() {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                String key = d.toString();
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("date", key);
                DayAgg a = days.get(key);
                if (a != null) {
                    m.put("pv", a.pv);
                    m.put("uv", a.ips.size());
                    m.put("sessions", a.sids.size());
                    m.put("avgDuration", a.durCnt <= 0 ? 0 : Math.round((double) a.durSum / a.durCnt));
                    m.put("src", "log");
                } else {
                    m.put("pv", 0);
                    m.put("uv", 0);
                    m.put("sessions", 0);
                    m.put("avgDuration", 0);
                    m.put("src", "none");
                }
                list.add(m);
            }
            return list;
        }

        private List<Map<String, Object>> hourly() {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (int h = 0; h < 24; h++) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("hour", h);
                m.put("name", String.format("%02d:00", h));
                m.put("pv", hourlyPv[h]);
                Set<String> s = hourlyUv.get(h);
                m.put("uv", s == null ? 0 : s.size());
                list.add(m);
            }
            return list;
        }

        private List<Map<String, Object>> pages() {
            List<Map.Entry<String, PathAgg>> es =
                    new ArrayList<Map.Entry<String, PathAgg>>(paths.entrySet());
            Collections.sort(es, new Comparator<Map.Entry<String, PathAgg>>() {
                @Override
                public int compare(Map.Entry<String, PathAgg> a, Map.Entry<String, PathAgg> b) {
                    int c = Long.compare(b.getValue().pv, a.getValue().pv);
                    return c != 0 ? c : a.getKey().compareTo(b.getKey());
                }
            });
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            int limit = Math.min(30, es.size());
            for (int i = 0; i < limit; i++) {
                Map.Entry<String, PathAgg> en = es.get(i);
                PathAgg a = en.getValue();
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("path", en.getKey());
                m.put("pv", a.pv);
                m.put("uv", a.ips.size());
                m.put("entries", a.entries);
                m.put("avgDuration", a.durCnt <= 0 ? 0 : Math.round((double) a.durSum / a.durCnt));
                list.add(m);
            }
            return list;
        }

        private static List<Map<String, Object>> distributions(Map<String, Long> m, boolean desc) {
            List<Map.Entry<String, Long>> es = new ArrayList<Map.Entry<String, Long>>(m.entrySet());
            Collections.sort(es, new Comparator<Map.Entry<String, Long>>() {
                @Override
                public int compare(Map.Entry<String, Long> a, Map.Entry<String, Long> b) {
                    if (desc) {
                        return Long.compare(b.getValue(), a.getValue());
                    }
                    // 来源/设备属于固定类别，按名称排序可让图表配色稳定不跳
                    return a.getKey().compareTo(b.getKey());
                }
            });
            long sum = 0;
            for (Map.Entry<String, Long> e : es) {
                sum += e.getValue();
            }
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (Map.Entry<String, Long> e : es) {
                Map<String, Object> o = new LinkedHashMap<String, Object>();
                o.put("name", e.getKey());
                o.put("value", e.getValue());
                o.put("pct", sum <= 0 ? 0 : Math.round(e.getValue() * 1000.0 / sum) / 10.0);
                list.add(o);
            }
            return list;
        }

        private List<Map<String, Object>> durationBuckets() {
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            long sum = 0;
            for (Long v : durBuckets.values()) {
                sum += v;
            }
            for (Map.Entry<String, Long> e : durBuckets.entrySet()) {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                m.put("name", e.getKey());
                m.put("value", e.getValue());
                m.put("pct", sum <= 0 ? 0 : Math.round(e.getValue() * 1000.0 / sum) / 10.0);
                list.add(m);
            }
            return list;
        }

        private Map<String, Object> health() {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("events", events);
            m.put("pvEvents", pvEvents);
            m.put("leaveEvents", leaveEvents);
            m.put("bot", botEvents);
            m.put("internal", internalEvents);
            m.put("truncated", truncated);
            m.put("logDays", days.size());
            return m;
        }

        // ---------- 小工具 ----------

        private static String dayOf(PageStatEvent e) {
            String st = e.getSt();
            return (st != null && st.length() >= 10) ? st.substring(0, 10) : null;
        }

        private static int hourOf(String st) {
            if (st == null || st.length() < 13) {
                return -1;
            }
            try {
                return Integer.parseInt(st.substring(11, 13));
            } catch (NumberFormatException e) {
                return -1;
            }
        }

        private static String bucketOf(long sec) {
            for (String[] b : DUR_BUCKETS) {
                long lo = Long.parseLong(b[1]);
                long hi = Long.parseLong(b[2]);
                if (sec >= lo && (hi < 0 || sec < hi)) {
                    return b[0];
                }
            }
            return DUR_BUCKETS[0][0];
        }

        private static void bump(Map<String, Long> m, String k) {
            Long v = m.get(k);
            m.put(k, v == null ? 1L : v + 1);
        }

        private static String nz(String s) {
            return s == null ? "" : s.trim();
        }

        private static String nz(String s, String dflt) {
            return (s == null || s.trim().isEmpty()) ? dflt : s.trim();
        }

        /** 从 "host/path" 或 "http://host/path" 里取出 host */
        static String hostOfRef(String ref) {
            String s = ref == null ? "" : ref.trim();
            int i = s.indexOf("://");
            if (i >= 0) {
                s = s.substring(i + 3);
            }
            int slash = s.indexOf('/');
            if (slash >= 0) {
                s = s.substring(0, slash);
            }
            int q = s.indexOf('?');
            if (q >= 0) {
                s = s.substring(0, q);
            }
            return s.isEmpty() ? "未知" : s;
        }

        /** 取出站点自身域名，并剥掉 www.——否则 referrer 里的裸域名会被误判成外部链接 */
        static String hostOfUrl(String url) {
            if (url == null || url.isEmpty()) {
                return "";
            }
            String s = url.trim().toLowerCase();
            int i = s.indexOf("://");
            if (i >= 0) {
                s = s.substring(i + 3);
            }
            int slash = s.indexOf('/');
            if (slash >= 0) {
                s = s.substring(0, slash);
            }
            int colon = s.indexOf(':');
            if (colon >= 0) {
                s = s.substring(0, colon);
            }
            if (s.startsWith("www.")) {
                s = s.substring(4);
            }
            return s;
        }
    }
}
