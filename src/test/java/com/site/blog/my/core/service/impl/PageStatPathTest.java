package com.site.blog.my.core.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 埋点入口的字段清洗规则。
 *
 * <p>这些是「队列之前的闸门」：路径不合法的请求在入队前就被丢掉，
 * 避免脏数据占掉有限队列容量；队列容量小是有意为之（只做削峰），
 * 被垃圾流量填满就等于真实访问被丢弃。
 */
class PageStatPathTest {

    @Test
    void 正常路径归一化() {
        assertEquals("/", PageStatLogServiceImpl.normalizePath("/"));
        assertEquals("/notes", PageStatLogServiceImpl.normalizePath("/notes"));
        assertEquals("/notes", PageStatLogServiceImpl.normalizePath("/notes/"), "去掉末尾斜杠，避免同一页面算两天");
        assertEquals("/notes/1", PageStatLogServiceImpl.normalizePath("/notes/1"));
        assertEquals("/notes/1", PageStatLogServiceImpl.normalizePath("/notes/1?from=wechat"));
        assertEquals("/notes/1", PageStatLogServiceImpl.normalizePath("/notes/1#comments"));
        assertEquals("/notes/1", PageStatLogServiceImpl.normalizePath("//notes//1"));
        assertEquals("/Cola-Glazed-Chicken", PageStatLogServiceImpl.normalizePath("/Cola-Glazed-Chicken"));
        assertEquals("/site-optimize", PageStatLogServiceImpl.normalizePath("/site-optimize"));
        // 分类名带中文，必须原样保留（表与列已是 utf8mb4）
        assertEquals("/category/工程化/1", PageStatLogServiceImpl.normalizePath("/category/工程化/1"));
    }

    /**
     * 回归护栏：站点路由（含中文分类名）必须全部通过。
     * <p>曾经的实现用 {@code ^[\p{Print}]*$} 做白名单，而 Java 默认模式下
     * {@code \p{Print}} 只覆盖 ASCII，中文路径被静默丢弃 —— 对中文站点等于丢掉半站流量。
     */
    @Test
    void 站点真实路由全部通过() {
        String[] routes = {
                "/", "/portfolio", "/notes", "/notes/page/2", "/notes/41", "/life", "/message",
                "/categories", "/category/工程化/1", "/category/做菜笔记/1", "/category/读书心得/1",
                "/tag/LangChain/1", "/search/agent/1", "/Cola-Glazed-Chicken", "/site-optimize",
                "/deepagents-02", "/notes/20"
        };
        for (String r : routes) {
            assertEquals(r, PageStatLogServiceImpl.normalizePath(r), "路由被误判为非法：" + r);
        }
        assertEquals("/category/工程化/1",
                PageStatLogServiceImpl.normalizePath("/category/工程化/1?from=wechat"),
                "中文路径 + query 也要能归一化");
    }

    @Test
    void 非法路径一律丢弃() {
        assertNull(PageStatLogServiceImpl.normalizePath(null));
        assertNull(PageStatLogServiceImpl.normalizePath(""));
        assertNull(PageStatLogServiceImpl.normalizePath("notes"), "必须是绝对路径");
        assertNull(PageStatLogServiceImpl.normalizePath("/a/../b"), "路径穿越");
        assertNull(PageStatLogServiceImpl.normalizePath("/notes/\n/x"), "控制字符");
        assertNull(PageStatLogServiceImpl.normalizePath("/admin"), "后台流量不计入前台统计");
        assertNull(PageStatLogServiceImpl.normalizePath("/admin/api/stat"));
        assertNull(PageStatLogServiceImpl.normalizePath("/blog/api/index/1"));
        assertNull(PageStatLogServiceImpl.normalizePath("/assets/index-a1b2c3.js"));
        assertNull(PageStatLogServiceImpl.normalizePath("/favicon.ico"));
        assertNull(PageStatLogServiceImpl.normalizePath("/sitemap.xml"));
        assertNull(PageStatLogServiceImpl.normalizePath("/" + repeat('a', 250)), "超长路径");
    }

    @Test
    void 来源页只留站与路径() {
        // 必须去掉 query：搜索引擎 referrer 里带着用户搜的关键词
        assertEquals("www.baidu.com/s", PageStatLogServiceImpl.normalizeRef("https://www.baidu.com/s?wd=私密关键词"));
        assertEquals("www.google.com/search", PageStatLogServiceImpl.normalizeRef("http://www.google.com/search?q=abc#top"));
        assertEquals("chengjungao.cn/notes", PageStatLogServiceImpl.normalizeRef("chengjungao.cn/notes"));
        assertNull(PageStatLogServiceImpl.normalizeRef(null));
        assertNull(PageStatLogServiceImpl.normalizeRef("   "));
    }

    @Test
    void IP脱敏模式() {
        assertEquals("1.2.3.*", PageStatLogServiceImpl.maskIp("1.2.3.4"));
        assertEquals("1.2.3.*,47.100.9.*", PageStatLogServiceImpl.maskChain("1.2.3.4, 47.100.9.9"));
        assertEquals("2001:db8:1:*", PageStatLogServiceImpl.maskIp("2001:db8:1:2::1"));
        assertEquals("", PageStatLogServiceImpl.maskIp(""));
        assertNull(PageStatLogServiceImpl.maskIp(null));
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
