package com.site.blog.my.core.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文章浏览量计数闸门。
 *
 * <p>锁定 2026-10-18 排查出的一条真实回归：文章浏览量曾在「nginx 路径校验 + try_files 内部重定向
 * 再次校验 + 详情接口」三处各自 +1，实测一次真实阅读 = +3；爬虫与预渲染也照样 +1（纯 curl 打一次
 * 文章 HTML 就 +1）。这些用例保证「非阅读场景一律不计数」。
 */
class ViewCountGuardTest {

    private static final String CHROME_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/141.0.0.0 Safari/537.36";

    private static MockHttpServletRequest req(String ua) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/deepagents-13");
        if (ua != null) {
            r.addHeader("User-Agent", ua);
        }
        // 默认模拟「公网访客经 CDN 回源」：链首真实 IP，链尾 CDN 节点
        r.addHeader("X-Forwarded-For", "1.2.3.4, 47.100.9.9");
        r.setRemoteAddr("172.17.0.5");
        return r;
    }

    @Test
    void 真实浏览器公网访客计数() {
        assertTrue(ViewCountGuard.shouldCount(req(CHROME_UA)));
    }

    @Test
    void 预渲染与无头浏览器不计数() {
        assertFalse(ViewCountGuard.shouldCount(req(
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "HeadlessChrome/141.0.0.0 Safari/537.36")), "预渲染不得计浏览量");
        assertFalse(ViewCountGuard.shouldCount(req(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/141.0.0.0 Safari/537.36 Puppeteer")));
    }

    @Test
    void 爬虫不计数() {
        assertFalse(ViewCountGuard.shouldCount(req(
                "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)")));
        assertFalse(ViewCountGuard.shouldCount(req(
                "Mozilla/5.0 (compatible; Bytespider; https://zhanzhang.toutiao.com/)")));
    }

    @Test
    void 脚本直连不计数() {
        // 空 UA / curl 默认 UA 都不是阅读
        assertFalse(ViewCountGuard.shouldCount(req(null)));
        assertFalse(ViewCountGuard.shouldCount(req("curl/8.4.0")));
    }

    @Test
    void 内网回环调用不计数() {
        MockHttpServletRequest r = req(CHROME_UA);
        r.removeHeader("X-Forwarded-For");
        r.setRemoteAddr("127.0.0.1");
        assertFalse(ViewCountGuard.shouldCount(r), "容器内预渲染/健康检查不得计浏览量");
    }

    @Test
    void 带预渲染标记的请求不计数() {
        MockHttpServletRequest r = req(CHROME_UA);
        r.addHeader("X-Prerender", "1");
        assertFalse(ViewCountGuard.shouldCount(r));
    }

    @Test
    void 空请求保守不计数() {
        assertFalse(ViewCountGuard.shouldCount(null));
    }
}
