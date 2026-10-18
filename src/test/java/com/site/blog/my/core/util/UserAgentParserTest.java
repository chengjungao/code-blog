package com.site.blog.my.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * UA 解析口径。爬虫判定是「统计准不准」的关键一环：
 * Googlebot 会执行 JS，页面里的埋点照跑，只能靠 UA 拦掉，
 * 否则它每天把站点爬一遍，PV 全是机器人。
 */
class UserAgentParserTest {

    private static final String CHROME_WIN =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/141.0.0.0 Safari/537.36";
    private static final String EDGE_WIN =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0";
    private static final String WECHAT_IOS =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Mobile/15E148 MicroMessenger/8.0.40(0x18002832) NetType/WIFI";

    @Test
    void 爬虫识别() {
        assertTrue(UserAgentParser.isBot(
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
        assertTrue(UserAgentParser.isBot(
                "Mozilla/5.0 (compatible; Baiduspider/2.0; +http://www.baidu.com/search/spider.html)"));
        assertTrue(UserAgentParser.isBot(
                "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"));
        assertTrue(UserAgentParser.isBot("python-requests/2.31.0"));
        assertTrue(UserAgentParser.isBot("curl/8.4.0"));
        // 预渲染/无头浏览器：不拦掉的话每天会被注入几十条幽灵 PV
        assertTrue(UserAgentParser.isBot(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 HeadlessChrome/140.0.0.0 Safari/537.36"));
        // 空 UA 一律按爬虫处理，正常浏览器不会没有 UA
        assertTrue(UserAgentParser.isBot(""));
        assertTrue(UserAgentParser.isBot(null));
        assertFalse(UserAgentParser.isBot(CHROME_WIN));
    }

    @Test
    void 爬虫标识与类别() {
        String googlebot = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)";
        assertEquals("Googlebot", UserAgentParser.botName(googlebot));
        assertEquals("search", UserAgentParser.botKind(googlebot));

        // Bytespider 里含 spider，必须先被具体规则认领，不能落到「未知爬虫」
        String bytespider = "Mozilla/5.0 (Linux; Android 5.0) AppleWebKit/537.36 "
                + "Chrome/70.0.3538.80 Mobile Safari/537.36; Bytespider";
        assertEquals("Bytespider", UserAgentParser.botName(bytespider));
        assertEquals("search", UserAgentParser.botKind(bytespider));

        String gptbot = "Mozilla/5.0 AppleWebKit/537.36; compatible; GPTBot/1.2";
        assertEquals("GPTBot", UserAgentParser.botName(gptbot));
        assertEquals("ai", UserAgentParser.botKind(gptbot), "AI 抓取要能与搜索引擎区分开");

        String headless = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
                + "HeadlessChrome/140.0.0.0 Safari/537.36";
        assertEquals("HeadlessChrome", UserAgentParser.botName(headless));
        assertEquals("automation", UserAgentParser.botKind(headless));

        assertEquals("Python-requests", UserAgentParser.botName("python-requests/2.31.0"));
        assertEquals("script", UserAgentParser.botKind("python-requests/2.31.0"));
        assertEquals("curl", UserAgentParser.botName("curl/8.4.0"));
        assertEquals("SemrushBot", UserAgentParser.botName("Mozilla/5.0 (compatible; SemrushBot/7~bl)"));
        assertEquals("seo", UserAgentParser.botKind("Mozilla/5.0 (compatible; SemrushBot/7~bl)"));

        // 空 UA 不是"未知"，而是一个明确事实：连 UA 都不发
        assertEquals("（空 UA）", UserAgentParser.botName(""));
        assertEquals("empty", UserAgentParser.botKind(null));

        // 泛化兜底：认得是机器，但说不出名字——不能因此当成真人放过去
        assertEquals("未知爬虫", UserAgentParser.botName("WeirdCrawlerTest/1.0 some-bot"));
        assertEquals("other", UserAgentParser.botKind("WeirdCrawlerTest/1.0 some-bot"));

        // 真实访客一个都不能被贴标识
        assertNull(UserAgentParser.botName(CHROME_WIN));
        assertNull(UserAgentParser.botKind(EDGE_WIN));
        assertNull(UserAgentParser.botName(WECHAT_IOS));
    }

    @Test
    void 浏览器识别() {
        assertEquals("Chrome", UserAgentParser.browser(CHROME_WIN));
        assertEquals("Edge", UserAgentParser.browser(EDGE_WIN), "Edg/ 必须先于 Chrome/ 判定");
        assertEquals("微信内置", UserAgentParser.browser(WECHAT_IOS));
        assertEquals("Firefox", UserAgentParser.browser("Mozilla/5.0 Firefox/142.0"));
        assertEquals("Safari", UserAgentParser.browser(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/17.0 Safari/605.1.15"));
        assertEquals("其他", UserAgentParser.browser("SomeRandomAgent/1.0"));
    }

    @Test
    void 系统识别() {
        assertEquals("Windows", UserAgentParser.os(CHROME_WIN));
        assertEquals("iOS", UserAgentParser.os(WECHAT_IOS));
        assertEquals("Android", UserAgentParser.os(
                "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 Chrome/141.0.0.0 Mobile Safari/537.36"));
        assertEquals("macOS", UserAgentParser.os(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Safari/605.1.15"));
    }

    @Test
    void 设备识别() {
        assertEquals("桌面端", UserAgentParser.device(CHROME_WIN));
        assertEquals("移动端", UserAgentParser.device(WECHAT_IOS));
        assertEquals("平板", UserAgentParser.device(
                "Mozilla/5.0 (iPad; CPU OS 16_0 like Mac OS X) AppleWebKit/605.1.15 Safari/604.1"));
    }
}
