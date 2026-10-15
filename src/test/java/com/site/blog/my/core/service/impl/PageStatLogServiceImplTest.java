package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.entity.PageStatEvent;
import com.site.blog.my.core.service.PageStatCounters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「有界内存队列 + 单消费者线程写文件」这条链路的端到端验证。
 *
 * <p>这里刻意不启动 Spring 上下文（也就不会碰线上库）：被测对象只依赖
 * {@link PageStatCounters} 和一个目录，用 {@code ReflectionTestUtils} 注入即可。
 * 目的只有一个——把「请求线程不会被拖住」和「数据真的落到了文件里」这两件事钉住。
 */
class PageStatLogServiceImplTest {

    @TempDir
    Path tmp;

    private PageStatLogServiceImpl newService(int capacity) {
        PageStatLogServiceImpl s = new PageStatLogServiceImpl();
        ReflectionTestUtils.setField(s, "logDir", tmp.toString());
        ReflectionTestUtils.setField(s, "capacity", capacity);
        ReflectionTestUtils.setField(s, "batchSize", 100);
        ReflectionTestUtils.setField(s, "retainDays", 90);
        ReflectionTestUtils.setField(s, "ipMode", "full");
        ReflectionTestUtils.setField(s, "enabled", true);
        ReflectionTestUtils.setField(s, "counters", new PageStatCounters());
        return s;
    }

    private static PageStatEvent event(String path, String ip) {
        PageStatEvent e = new PageStatEvent();
        e.setType("pv");
        e.setPath(path);
        e.setIp(ip);
        e.setIpSrc("xff");
        e.setUa("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/141.0.0.0 Safari/537.36");
        e.setVid("v1");
        e.setSid("s1");
        e.setEntry(true);
        e.setRef("https://www.baidu.com/s?wd=x");
        return e;
    }

    private static Map<String, Object> countersOf(PageStatLogServiceImpl s) {
        PageStatCounters c = (PageStatCounters) ReflectionTestUtils.getField(s, "counters");
        return c.todaySnapshot();
    }

    private static void waitFor(String what, BooleanSupplier cond) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (cond.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        throw new AssertionError("等待超时：" + what);
    }

    @Test
    void 事件被批量落盘为JSONL且可回读() throws Exception {
        PageStatLogServiceImpl s = newService(2000);
        try {
            s.start();
            for (int i = 0; i < 20; i++) {
                assertTrue(s.offer(event("/notes/" + i, "1.2.3." + i)));
            }
            waitFor("20 条写完", () -> (Long) s.health().get("written") >= 20L);

            List<String> files = s.files();
            assertEquals(1, files.size(), "同一天只应有一个明细文件");
            assertEquals("stat-" + LocalDate.now() + ".jsonl", files.get(0));

            List<String> lines = Files.readAllLines(tmp.resolve(files.get(0)), StandardCharsets.UTF_8);
            assertEquals(20, lines.size(), "一行一条事件");
            String first = lines.get(0);
            assertTrue(first.startsWith("{") && first.endsWith("}"), "必须是合法 JSON Lines");
            // 服务端补全的维度必须落到明细里，否则报表没有维度可切
            assertTrue(first.contains("\"browser\":\"Chrome\""), "UA 解析结果应落在明细里");
            assertTrue(first.contains("\"os\":\"Windows\""));
            assertTrue(first.contains("\"device\":\"桌面端\""));
            assertTrue(first.contains("\"st\":\""), "服务端时间必须存在");
            assertTrue(first.contains("\"ipSrc\":\"xff\""), "IP 取值来源必须可核对");
            assertFalse(first.contains("wd="), "referrer 的 query 必须剥掉（里面是用户搜索关键词）");

            List<PageStatEvent> back = new ArrayList<PageStatEvent>();
            s.forEachEvent(LocalDate.now(), LocalDate.now(), back::add);
            assertEquals(20, back.size(), "forEachEvent 应能流式回读全部明细");
            assertEquals("/notes/7", back.get(7).getPath());
            assertEquals("1.2.3.7", back.get(7).getIp());
            assertTrue(back.get(7).isEntry());
            assertFalse(back.get(7).isBot());
        } finally {
            s.stop();
        }
    }

    @Test
    void 有效访问进内存计数而爬虫与内网不进() {
        PageStatLogServiceImpl s = newService(2000);
        try {
            s.start();
            assertTrue(s.offer(event("/notes/1", "1.2.3.4")));

            PageStatEvent bot = event("/notes/2", "1.2.3.5");
            bot.setUa("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)");
            assertTrue(s.offer(bot));

            // 预渲染直连容器内 nginx，来源必然是回环地址
            PageStatEvent inner = event("/notes/3", "127.0.0.1");
            inner.setInternal(true);
            assertTrue(s.offer(inner));

            waitFor("3 条写完", () -> (Long) s.health().get("written") >= 3L);

            assertEquals(1L, s.health().get("bot"), "爬虫要计数，供「数据健康」核对");
            assertEquals(1L, s.health().get("internal"), "内网要计数");

            Map<String, Object> snap = countersOf(s);
            assertEquals(1L, snap.get("pv"), "只有真实访问计入 PV");
            assertEquals(1, snap.get("uv"), "UV 也不该把爬虫/内网算进去");
        } finally {
            s.stop();
        }
    }

    @Test
    void 队列满时丢弃而不是阻塞请求线程() {
        PageStatLogServiceImpl s = newService(100);
        s.start();
        s.stop(); // 只留队列、不放消费者：模拟下游写盘彻底卡住的极端情况

        long begin = System.currentTimeMillis();
        int accepted = 0;
        for (int i = 0; i < 300; i++) {
            if (s.offer(event("/notes/" + i, "1.2.3.4"))) {
                accepted++;
            }
        }
        long cost = System.currentTimeMillis() - begin;

        assertEquals(100, accepted, "有界队列必须严格按容量收下");
        assertEquals(200L, s.health().get("dropped"), "溢出的必须计数，不能静默消失");
        assertTrue(cost < 1000, "写盘卡住时 offer 也必须立刻返回，实耗 " + cost + "ms");
    }

    @Test
    void 非法路径的埋点被拒且不计入统计() {
        PageStatLogServiceImpl s = newService(2000);
        try {
            s.start();
            assertTrue(s.offer(event("/admin/api/stat", "1.2.3.4")));
            assertTrue(s.offer(event("/assets/app.js", "1.2.3.4")));
            waitFor("非法路径被拒", () -> (Long) s.health().get("rejected") >= 2L);
            assertEquals(0L, countersOf(s).get("pv"), "后台与静态资源流量不能污染前台统计");
        } finally {
            s.stop();
        }
    }

    @Test
    void 过期明细文件按保留期清理() throws Exception {
        PageStatLogServiceImpl s = newService(2000);
        Path old = tmp.resolve("stat-2020-01-01.jsonl");
        Path keep = tmp.resolve("stat-" + LocalDate.now() + ".jsonl");
        Path alien = tmp.resolve("notes.txt");
        Files.write(old, "x".getBytes(StandardCharsets.UTF_8));
        Files.write(keep, "x".getBytes(StandardCharsets.UTF_8));
        Files.write(alien, "x".getBytes(StandardCharsets.UTF_8));

        s.cleanExpired();

        assertFalse(Files.exists(old), "超过保留期的明细应被删除");
        assertTrue(Files.exists(keep), "保留期内的明细不能被删");
        assertTrue(Files.exists(alien), "不是本服务产出的文件一律不准动");
    }
}
