package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.PageStatMapper;
import com.site.blog.my.core.entity.PageStatEvent;
import com.site.blog.my.core.service.PageStatCounters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多维报表聚合口径。
 *
 * <p>DB 用 mock 顶掉：这里要验的是「明细怎么变成报表」，不是「MySQL 好不好用」。
 * 同时顺带验证「明细缺失 → 回退 DB 并标注 src=db」这条降级路径。
 */
class VisitStatReportTest {

    private static final String DAY1 = "2026-10-01";
    private static final String DAY2 = "2026-10-02";
    private static final String DAY3 = "2026-10-03";

    @TempDir
    Path tmp;

    private PageStatLogServiceImpl log;
    private VisitStatServiceImpl svc;

    @BeforeEach
    void setUp() {
        log = new PageStatLogServiceImpl();
        ReflectionTestUtils.setField(log, "logDir", tmp.toString());
        ReflectionTestUtils.setField(log, "capacity", 2000);
        ReflectionTestUtils.setField(log, "batchSize", 200);
        ReflectionTestUtils.setField(log, "retainDays", 90);
        ReflectionTestUtils.setField(log, "ipMode", "full");
        ReflectionTestUtils.setField(log, "enabled", true);
        ReflectionTestUtils.setField(log, "counters", new PageStatCounters());
        log.start();

        PageStatMapper mapper = Mockito.mock(PageStatMapper.class);
        // DAY3 没有明细，只能来自 DB —— 用来验证降级路径
        List<Map<String, Object>> dbRows = new ArrayList<Map<String, Object>>();
        Map<String, Object> row = new HashMap<String, Object>();
        row.put("statdate", DAY3);
        row.put("pv", 5);
        row.put("uv", 3);
        dbRows.add(row);
        Mockito.when(mapper.selectDailySummary(Mockito.anyString(), Mockito.anyString())).thenReturn(dbRows);

        svc = new VisitStatServiceImpl();
        ReflectionTestUtils.setField(svc, "pageStatLogService", log);
        ReflectionTestUtils.setField(svc, "pageStatMapper", mapper);
        ReflectionTestUtils.setField(svc, "counters", new PageStatCounters());
        ReflectionTestUtils.setField(svc, "baseUrl", "https://www.chengjungao.cn");
    }

    private PageStatEvent e(String day, String hour, String type, String path, String ip,
                            String vid, String sid, boolean entry, String ref, Long dur) {
        PageStatEvent ev = new PageStatEvent();
        ev.setType(type);
        ev.setPath(path);
        ev.setIp(ip);
        ev.setVid(vid);
        ev.setSid(sid);
        ev.setEntry(entry);
        ev.setRef(ref);
        ev.setDur(dur);
        ev.setSt(day + " " + hour + ":00:00");
        ev.setUa("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/141.0.0.0 Safari/537.36");
        return ev;
    }

    private void feed(PageStatEvent... events) {
        for (PageStatEvent ev : events) {
            log.offer(ev);
        }
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline
                && (Long) log.health().get("written") < events.length) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> list(Map<String, Object> r, String key) {
        return (List<Map<String, Object>>) r.get(key);
    }

    private static Object findValue(List<Map<String, Object>> list, String nameKey, String name, String valueKey) {
        for (Map<String, Object> m : list) {
            if (name.equals(m.get(nameKey))) {
                return m.get(valueKey);
            }
        }
        return null;
    }

    @Test
    void 各维度聚合口径() {
        feed(
                // ---- DAY1 10 点：直接访问落地 /notes，同会话又看了 /notes/1 ----
                e(DAY1, "10", "pv", "/notes", "1.1.1.1", "A", "S1", true, "", null),
                e(DAY1, "10", "pv", "/notes/1", "1.1.1.1", "A", "S1", false, "", null),
                e(DAY1, "10", "leave", "/notes", "1.1.1.1", "A", "S1", false, "", 40L),
                // ---- DAY1 10 点：从百度搜索落地 /life ----
                e(DAY1, "10", "pv", "/life", "2.2.2.2", "B", "S2", true, "https://www.baidu.com/s?wd=私密", null),
                e(DAY1, "10", "leave", "/life", "2.2.2.2", "B", "S2", false, "", 200L),
                // ---- DAY2 20 点：老访客 A 回来了 + 新访客 C 从 Google 来 ----
                e(DAY2, "20", "pv", "/notes/1", "1.1.1.1", "A", "S1", true, "", null),
                e(DAY2, "20", "pv", "/notes/1", "3.3.3.3", "C", "S3", true, "https://www.google.com/", null));

        Map<String, Object> r = svc.getStatReport(DAY1, DAY3);

        // ---- summary ----
        Map<String, Object> s = (Map<String, Object>) r.get("summary");
        // summary.pv 是「明细 + DB 回退」的合并总量；leave 事件不算 PV 这一点看 pvLog
        assertEquals(5L, s.get("pvLog"), "leave 事件不算 PV（纯明细口径）");
        assertEquals(3, s.get("uv"), "按 IP 去重");
        assertEquals(3, s.get("sessions"), "3 个会话");
        assertEquals(4L, s.get("entries"), "4 次落地");
        assertEquals(120L, s.get("avgDuration"), "(40+200)/2");
        assertEquals(66.7, s.get("bounceRate"), "S2/S3 只看了 1 页 → 2/3");
        assertEquals(1.67, s.get("pagesPerSession"), "5/3 保留两位");
        assertEquals(2, s.get("newVisitors"), "B、C 只出现过 1 天");
        assertEquals(1, s.get("returningVisitors"), "A 跨两天出现");

        // ---- trend：明细优先 + DB 兜底并标注来源 ----
        List<Map<String, Object>> trend = list(r, "trend");
        assertEquals(3, trend.size());
        assertEquals(3L, trend.get(0).get("pv"));
        assertEquals(2, trend.get(0).get("uv"));
        assertEquals("log", trend.get(0).get("src"));
        assertEquals(2L, trend.get(1).get("pv"));
        assertEquals(5L, trend.get(2).get("pv"), "DAY3 无明细，回退 DB");
        assertEquals("db", trend.get(2).get("src"), "来源必须标注，不能与明细口径混在一起");

        // ---- hourly ----
        List<Map<String, Object>> hourly = list(r, "hourly");
        assertEquals(24, hourly.size());
        assertEquals(3L, findValue(hourly, "name", "10:00", "pv"));
        assertEquals(2L, findValue(hourly, "name", "20:00", "pv"));
        assertEquals(0L, findValue(hourly, "name", "03:00", "pv"));

        // ---- pages：PV 倒序，带落地次数与平均停留 ----
        List<Map<String, Object>> pages = list(r, "pages");
        assertEquals("/notes/1", pages.get(0).get("path"));
        assertEquals(3L, pages.get(0).get("pv"));
        assertEquals(2L, pages.get(0).get("entries"), "/notes/1 两次作为落地页");
        assertEquals(1L, findValue(pages, "path", "/notes", "pv"));
        assertEquals(40L, findValue(pages, "path", "/notes", "avgDuration"), "只有 /notes 有停留采样");
        assertEquals(200L, findValue(pages, "path", "/life", "avgDuration"));

        // ---- sources：落地页来源分类 ----
        List<Map<String, Object>> sources = list(r, "sources");
        assertEquals(2L, findValue(sources, "name", "直接访问", "value"));
        assertEquals(2L, findValue(sources, "name", "搜索引擎", "value"));
        assertEquals(50.0, findValue(sources, "name", "直接访问", "pct"));
        // referrer 被剥成 host+path（query 里的搜索词不留痕），来源榜只按站统计
        assertEquals(1L, findValue(list(r, "sourceHosts"), "name", "www.baidu.com", "value"));
        assertEquals(1L, findValue(list(r, "sourceHosts"), "name", "www.google.com", "value"));

        // ---- 终端画像 ----
        assertEquals(5L, findValue(list(r, "devices"), "name", "桌面端", "value"));
        assertEquals(5L, findValue(list(r, "browsers"), "name", "Chrome", "value"));
        assertEquals(5L, findValue(list(r, "systems"), "name", "Windows", "value"));

        // ---- 停留时长分布 ----
        List<Map<String, Object>> durations = list(r, "durations");
        assertEquals(6, durations.size());
        assertEquals(1L, findValue(durations, "name", "30-60 秒", "value"));
        assertEquals(1L, findValue(durations, "name", "3-10 分钟", "value"));
        assertEquals(0L, findValue(durations, "name", "10 分钟以上", "value"));

        // ---- 健康度 ----
        Map<String, Object> health = (Map<String, Object>) r.get("health");
        assertEquals(7L, health.get("events"), "所有事件都写进了明细");
        assertEquals(5L, health.get("pvEvents"));
        assertEquals(2L, health.get("leaveEvents"));
        assertEquals(2, health.get("logDays"), "只有 DAY1/DAY2 有明细");
        assertEquals(1, health.get("dbFallbackDays"), "DAY3 走 DB 回退");

        // ---- 兼容旧字段 ----
        assertEquals(3, list(r, "daily").size());
        assertEquals(3, list(r, "topPages").size());
        Map<String, Object> total = (Map<String, Object>) r.get("total");
        assertEquals(10L, total.get("pv"), "明细 5 + DB 回退 5");
        assertEquals(3, total.get("uv"));

        // 回归护栏：报表卡片读的是 summary.pv，不是 total.pv。
        // 曾经只更新 total，导致「明细上线前」的 PV 在卡片上完全看不到（线上少算 95%）。
        Map<String, Object> summary = (Map<String, Object>) r.get("summary");
        assertEquals(10L, summary.get("pv"), "summary.pv 必须包含 DB 回退的量");
        assertEquals(5L, summary.get("pvLog"), "pvLog 保留纯明细口径，便于自证");

        assertNotNull(r.get("range"));
        assertNotNull(r.get("realtime"));
        assertNotNull(r.get("queue"));
        assertNotNull(r.get("files"));
        assertTrue((Boolean) log.health().get("consumerAlive"));
    }

    @Test
    void 爬虫与内网流量写进明细但不进报表() {
        PageStatEvent real = e(DAY1, "10", "pv", "/notes", "1.1.1.1", "A", "S1", true, "", null);
        PageStatEvent bot = e(DAY1, "10", "pv", "/notes", "1.1.1.2", "B", "S2", true, "", null);
        bot.setUa("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
        PageStatEvent inner = e(DAY1, "10", "pv", "/notes", "127.0.0.1", "C", "S3", true, "", null);
        inner.setInternal(true);

        feed(real, bot, inner);

        Map<String, Object> r = svc.getStatReport(DAY1, DAY1);
        Map<String, Object> s = (Map<String, Object>) r.get("summary");
        assertEquals(1L, s.get("pv"), "只有真实访客计入 PV");
        assertEquals(1, s.get("uv"));
        assertEquals(1, s.get("sessions"));

        Map<String, Object> health = (Map<String, Object>) r.get("health");
        assertEquals(3L, health.get("events"), "三条都写进了明细，可事后核对");
        assertEquals(1L, health.get("bot"));
        assertEquals(1L, health.get("internal"));

        List<Map<String, Object>> sources = list(r, "sources");
        assertEquals(1, sources.size(), "爬虫/内网不产生来源数据");
    }

    @Test
    void 空区间不报错且结构完整() {
        Map<String, Object> r = svc.getStatReport("2026-01-01", "2026-01-03");
        Map<String, Object> s = (Map<String, Object>) r.get("summary");
        assertEquals(0L, s.get("pv"));
        assertEquals(0, s.get("uv"));
        assertEquals(0.0, s.get("bounceRate"), "没有会话时不能除零");
        assertEquals(0.0, s.get("pagesPerSession"));
        assertEquals(3, list(r, "trend").size());
        assertEquals(24, list(r, "hourly").size());
        assertEquals(6, list(r, "durations").size());
        assertTrue(list(r, "pages").isEmpty());
    }

    @Test
    void 区间超过上限时自动截断() {
        Map<String, Object> r = svc.getStatReport("2020-01-01", DAY3);
        Map<String, Object> range = (Map<String, Object>) r.get("range");
        assertEquals(90, range.get("days"), "最多回溯 90 天，防止一次查询把 JVM 拖垮");
        assertEquals(DAY3, range.get("end"));
    }

    @Test
    void 起止颠倒时自动纠正() {
        Map<String, Object> r = svc.getStatReport(DAY3, DAY1);
        Map<String, Object> range = (Map<String, Object>) r.get("range");
        assertEquals(DAY1, range.get("start"));
        assertEquals(DAY3, range.get("end"));
    }
}
