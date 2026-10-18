package com.site.blog.my.core.util;

import javax.servlet.http.HttpServletRequest;

/**
 * 文章浏览量的计数闸门。
 *
 * <p>为什么需要它：{@code tb_blog.blog_views} 原先在三个位置各自 +1 ——
 * ① nginx {@code auth_request /_validate_path} 的路径合法性校验（自定义 slug 走
 * {@code getBlogDetailBySubUrl}）；② 该 location 的 {@code try_files} 内部重定向会再次触发
 * 校验；③ 前端详情接口。于是一次真实阅读被记 3 次（2026-10-18 实测：无头 Chrome 单次访问
 * 文章浏览量 +3）。再叠加每日 02:00 的全量预渲染（40 篇 ×3）与各类爬虫，累计值会远高于真实阅读量。
 *
 * <p>判定规则与前端 {@code utils/tracker.js} 保持同一套口径：只有「真实浏览器」的一次请求才计数。
 */
public final class ViewCountGuard {

    /** 真实浏览器 UA 都很长，低于该长度视为脚本直连 */
    private static final int MIN_UA_LENGTH = 40;

    private ViewCountGuard() {
    }

    /**
     * 该请求是否应该计入文章浏览量。
     *
     * @param request 当前请求，可为 null（null 时保守地不计数）
     * @return true 表示是真实浏览器发出的请求
     */
    public static boolean shouldCount(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        // 预渲染器（render.js）如带标记则直接排除，避免它冒充访客
        if (request.getHeader("X-Prerender") != null) {
            return false;
        }
        String ua = request.getHeader("User-Agent");
        if (ua == null || ua.trim().length() < MIN_UA_LENGTH) {
            return false;
        }
        // 无头浏览器：预渲染、各类截图/监控服务
        if (ua.contains("HeadlessChrome") || ua.contains("Puppeteer") || ua.contains("Playwright")) {
            return false;
        }
        // 爬虫 / 自动化（规则表见 UserAgentParser，与统计报表共用同一份规则）
        if (UserAgentParser.isBot(ua)) {
            return false;
        }
        // 内网/回环地址 = 自己人（容器内预渲染、健康检查、监控探针）
        return !ClientIpResolver.resolve(request).isInternal();
    }
}
