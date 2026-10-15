package com.site.blog.my.core.util;

import java.util.regex.Pattern;

/**
 * 轻量 User-Agent 解析：只做统计需要的四件事——是否爬虫、浏览器、操作系统、设备类型。
 * 不引第三方依赖，口径集中在这里，报表口径要改只改这一处。
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    /** 爬虫 / 自动化 / 压测工具。Googlebot 会执行 JS，必须靠 UA 在这里拦掉，否则计入 PV。 */
    private static final Pattern BOT = Pattern.compile(
            "bot|crawler|crawling|spider|slurp|bingpreview|facebookexternalhit|whatsapp|telegrambot|"
                    + "python-requests|python-urllib|aiohttp|scrapy|curl/|wget|httpclient|okhttp|go-http-client|"
                    + "java/|libwww|headless|phantomjs|lighthouse|puppeteer|playwright|"
                    + "semrush|ahrefs|mj12bot|dotbot|bytespider|petalbot|yisouspider|yandex|megaindex|"
                    + "exabot|ia_archiver|archive\\.org_bot|scan|monitor|uptime|pingdom|nagios",
            Pattern.CASE_INSENSITIVE);

    public static boolean isBot(String ua) {
        return ua == null || ua.trim().isEmpty() || BOT.matcher(ua).find();
    }

    public static String browser(String ua) {
        if (ua == null || ua.isEmpty()) {
            return "未知";
        }
        if (ua.contains("MicroMessenger")) {
            return "微信内置";
        }
        if (ua.contains("QQBrowser")) {
            return "QQ浏览器";
        }
        if (ua.contains("UCBrowser") || ua.contains("UBrowser")) {
            return "UC浏览器";
        }
        if (ua.contains("QihooBrowser") || ua.contains("360SE") || ua.contains("360EE")) {
            return "360浏览器";
        }
        if (ua.contains("Quark")) {
            return "夸克";
        }
        if (ua.contains("Edg/") || ua.contains("EdgA/") || ua.contains("EdgiOS/")) {
            return "Edge";
        }
        if (ua.contains("OPR/") || ua.contains("Opera")) {
            return "Opera";
        }
        if (ua.contains("Firefox/") || ua.contains("FxiOS/")) {
            return "Firefox";
        }
        if (ua.contains("Chrome/") || ua.contains("CriOS/")) {
            return "Chrome";
        }
        if (ua.contains("Safari/")) {
            return "Safari";
        }
        return "其他";
    }

    public static String os(String ua) {
        if (ua == null || ua.isEmpty()) {
            return "未知";
        }
        if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iPod")) {
            return "iOS";
        }
        if (ua.contains("Android")) {
            return "Android";
        }
        if (ua.contains("Windows NT")) {
            return "Windows";
        }
        if (ua.contains("Mac OS X") || ua.contains("Macintosh")) {
            return "macOS";
        }
        if (ua.contains("HarmonyOS")) {
            return "HarmonyOS";
        }
        if (ua.contains("Linux")) {
            return "Linux";
        }
        return "其他";
    }

    public static String device(String ua) {
        if (ua == null || ua.isEmpty()) {
            return "未知";
        }
        if (ua.contains("iPad") || ua.contains("Tablet") || ua.contains("Pad")) {
            return "平板";
        }
        if (ua.contains("Mobile") || ua.contains("Android") || ua.contains("iPhone") || ua.contains("iPod")) {
            return "移动端";
        }
        return "桌面端";
    }
}
