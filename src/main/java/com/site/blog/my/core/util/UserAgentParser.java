package com.site.blog.my.core.util;

import java.util.regex.Pattern;

/**
 * 轻量 User-Agent 解析：只做统计需要的几件事——爬虫识别与标识、浏览器、操作系统、设备类型。
 * 不引第三方依赖，口径集中在这里，报表口径要改只改这一处。
 *
 * <p>爬虫这块分两层输出：
 * <ul>
 *   <li>{@link #isBot(String)}——是否机器流量，报表主口径据此剔除；</li>
 *   <li>{@link #botName(String)} / {@link #botKind(String)}——具体是谁、属于哪一类，
 *       写进明细供「爬虫统计」单独核算，让人与机器两条曲线可以对照。</li>
 * </ul>
 * 三者共用同一份规则表，不会出现「说它是爬虫但说不出是谁」或反过来的情况。
 */
public final class UserAgentParser {

    private UserAgentParser() {
    }

    // ==================== 爬虫类别 ====================

    /** 搜索引擎 */
    public static final String KIND_SEARCH = "search";
    /** AI 训练 / 检索抓取 */
    public static final String KIND_AI = "ai";
    /** SEO 与站点分析工具 */
    public static final String KIND_SEO = "seo";
    /** 可用性监控 */
    public static final String KIND_MONITOR = "monitor";
    /** 社交平台链接预览 */
    public static final String KIND_SOCIAL = "social";
    /** 脚本 / HTTP 客户端 */
    public static final String KIND_SCRIPT = "script";
    /** 无头浏览器 / 自动化测试 */
    public static final String KIND_AUTOMATION = "automation";
    /** 安全扫描器 */
    public static final String KIND_SCANNER = "scanner";
    /** 空 UA（正常浏览器不会没有 UA） */
    public static final String KIND_EMPTY = "empty";
    /** 其他（泛化规则兜底命中） */
    public static final String KIND_OTHER = "other";

    /** 单条识别规则：正则 + 展示名 + 类别 */
    private static final class Rule {
        final Pattern pattern;
        final String name;
        final String kind;

        Rule(String regex, String name, String kind) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            this.name = name;
            this.kind = kind;
        }
    }

    private static Rule r(String regex, String name, String kind) {
        return new Rule(regex, name, kind);
    }

    /** 空 UA 的占位标识 */
    private static final String NAME_EMPTY = "（空 UA）";

    /** 泛化规则命中的兜底标识 */
    private static final String NAME_UNKNOWN = "未知爬虫";

    /**
     * 具体爬虫规则。<b>顺序敏感</b>：越具体的越靠前（Bytespider 必须早于泛化的 spider）。
     */
    private static final Rule[] RULES = buildRules();

    private static Rule[] buildRules() {
        return new Rule[]{
                // ---------- 搜索引擎 ----------
                r("Googlebot", "Googlebot", KIND_SEARCH),
                r("Google-InspectionTool", "Google 检查工具", KIND_SEARCH),
                r("Storebot-Google", "Storebot", KIND_SEARCH),
                r("bingbot|MSNBot", "Bingbot", KIND_SEARCH),
                r("Baiduspider", "Baiduspider", KIND_SEARCH),
                r("Sogou\\s*(web\\s*)?spider|SogouSpider", "Sogou Spider", KIND_SEARCH),
                r("360Spider|HaosouSpider", "360Spider", KIND_SEARCH),
                r("YisouSpider", "YisouSpider", KIND_SEARCH),
                r("Bytespider", "Bytespider", KIND_SEARCH),
                r("PetalBot", "PetalBot", KIND_SEARCH),
                r("YandexBot|YandexImages|YandexMetrika", "YandexBot", KIND_SEARCH),
                r("DuckDuckBot", "DuckDuckBot", KIND_SEARCH),
                r("Applebot", "Applebot", KIND_SEARCH),
                r("Slurp", "Yahoo! Slurp", KIND_SEARCH),
                r("Exabot", "Exabot", KIND_SEARCH),
                r("ia_archiver", "Alexa Archiver", KIND_SEARCH),

                // ---------- AI 训练 / 检索抓取 ----------
                r("GPTBot", "GPTBot", KIND_AI),
                r("OAI-SearchBot", "OAI-SearchBot", KIND_AI),
                r("ChatGPT-User", "ChatGPT-User", KIND_AI),
                r("ClaudeBot|Claude-Web|anthropic", "ClaudeBot", KIND_AI),
                r("CCBot", "CCBot", KIND_AI),
                r("PerplexityBot", "PerplexityBot", KIND_AI),
                r("Amazonbot", "Amazonbot", KIND_AI),
                r("Meta-ExternalAgent", "Meta-ExternalAgent", KIND_AI),
                r("Diffbot", "Diffbot", KIND_AI),

                // ---------- 社交平台链接预览 ----------
                r("facebookexternalhit|facebookcatalog", "Facebook", KIND_SOCIAL),
                r("Twitterbot", "Twitterbot", KIND_SOCIAL),
                r("TelegramBot", "TelegramBot", KIND_SOCIAL),
                r("WhatsApp", "WhatsApp", KIND_SOCIAL),
                r("Slackbot|Slack-ImgProxy", "Slackbot", KIND_SOCIAL),
                r("Discordbot", "Discordbot", KIND_SOCIAL),
                r("LinkedInBot", "LinkedInBot", KIND_SOCIAL),
                r("Pinterestbot", "Pinterestbot", KIND_SOCIAL),

                // ---------- SEO / 站点分析 ----------
                r("SemrushBot", "SemrushBot", KIND_SEO),
                r("AhrefsBot", "AhrefsBot", KIND_SEO),
                r("MJ12bot", "MJ12bot", KIND_SEO),
                r("DotBot", "DotBot", KIND_SEO),
                r("MegaIndex", "MegaIndex", KIND_SEO),
                r("Screaming Frog", "Screaming Frog", KIND_SEO),
                r("DataForSeoBot", "DataForSeoBot", KIND_SEO),

                // ---------- 可用性监控 ----------
                r("UptimeRobot", "UptimeRobot", KIND_MONITOR),
                r("Pingdom", "Pingdom", KIND_MONITOR),
                r("StatusCake", "StatusCake", KIND_MONITOR),
                r("Site24x7", "Site24x7", KIND_MONITOR),
                r("Nagios", "Nagios", KIND_MONITOR),
                r("Better ?Uptime", "Better Uptime", KIND_MONITOR),

                // ---------- 无头浏览器 / 自动化 ----------
                r("HeadlessChrome", "HeadlessChrome", KIND_AUTOMATION),
                r("Puppeteer", "Puppeteer", KIND_AUTOMATION),
                r("Playwright", "Playwright", KIND_AUTOMATION),
                r("PhantomJS", "PhantomJS", KIND_AUTOMATION),
                r("Selenium", "Selenium", KIND_AUTOMATION),
                r("Lighthouse", "Lighthouse", KIND_AUTOMATION),

                // ---------- 安全扫描 ----------
                r("sqlmap", "sqlmap", KIND_SCANNER),
                r("Nikto", "Nikto", KIND_SCANNER),
                r("Nmap|masscan", "Nmap/masscan", KIND_SCANNER),
                r("zgrab", "zgrab", KIND_SCANNER),
                r("Censys", "Censys", KIND_SCANNER),
                r("Shodan", "Shodan", KIND_SCANNER),
                r("Nuclei", "Nuclei", KIND_SCANNER),
                r("Acunetix|Netsparker|AppScan|AWVS", "漏洞扫描器", KIND_SCANNER),
                r("Gobuster|DirBuster|feroxbuster|ffuf", "目录扫描器", KIND_SCANNER),

                // ---------- 脚本 / HTTP 客户端 ----------
                r("curl/", "curl", KIND_SCRIPT),
                r("Wget", "Wget", KIND_SCRIPT),
                r("python-requests", "Python-requests", KIND_SCRIPT),
                r("python-urllib", "Python-urllib", KIND_SCRIPT),
                r("aiohttp", "aiohttp", KIND_SCRIPT),
                r("httpx", "HTTPX", KIND_SCRIPT),
                r("Scrapy", "Scrapy", KIND_SCRIPT),
                r("Go-http-client", "Go-http-client", KIND_SCRIPT),
                r("okhttp", "OkHttp", KIND_SCRIPT),
                r("httpclient", "HttpClient", KIND_SCRIPT),
                r("libwww", "libwww", KIND_SCRIPT),
                r("Java/", "Java", KIND_SCRIPT),
                r("axios/", "axios", KIND_SCRIPT),
                r("node-fetch", "node-fetch", KIND_SCRIPT),
                r("PostmanRuntime", "Postman", KIND_SCRIPT),
                r("insomnia", "Insomnia", KIND_SCRIPT),
        };
    }

    /**
     * 泛化兜底规则（原有口径，一条不删）：上面规则表没认出具体名字、但明显是机器的 UA
     * 在这里命中，计入「未知爬虫」。保留它才能保证「以前判为爬虫的，现在仍然算爬虫」。
     */
    private static final Pattern GENERIC_BOT = Pattern.compile(
            "bot|crawler|crawling|spider|slurp|bingpreview|facebookexternalhit|whatsapp|telegrambot|"
                    + "python-requests|python-urllib|aiohttp|scrapy|curl/|wget|httpclient|okhttp|go-http-client|"
                    + "java/|libwww|headless|phantomjs|lighthouse|puppeteer|playwright|"
                    + "semrush|ahrefs|mj12bot|dotbot|bytespider|petalbot|yisouspider|yandex|megaindex|"
                    + "exabot|ia_archiver|archive\\.org_bot|scan|monitor|uptime|pingdom|nagios",
            Pattern.CASE_INSENSITIVE);

    private static final Rule EMPTY = new Rule("$^", NAME_EMPTY, KIND_EMPTY);
    private static final Rule UNKNOWN = new Rule("$^", NAME_UNKNOWN, KIND_OTHER);

    /** 命中一条规则就返回；空 UA 直接判空 UA；都不中再走泛化兜底；仍不中则不是爬虫 */
    private static Rule match(String ua) {
        if (ua == null || ua.trim().isEmpty()) {
            return EMPTY;
        }
        for (Rule rule : RULES) {
            if (rule.pattern.matcher(ua).find()) {
                return rule;
            }
        }
        return GENERIC_BOT.matcher(ua).find() ? UNKNOWN : null;
    }

    /** 是否机器流量。Googlebot 会执行 JS，必须靠 UA 拦掉，否则每天把站点爬一遍，PV 全是机器人。 */
    public static boolean isBot(String ua) {
        return match(ua) != null;
    }

    /**
     * 爬虫标识（如 Googlebot / GPTBot / Python-requests）；非爬虫返回 {@code null}。
     * 报表里「谁在抓」就是靠这个字段排序的。
     */
    public static String botName(String ua) {
        Rule rule = match(ua);
        return rule == null ? null : rule.name;
    }

    /**
     * 爬虫类别（search / ai / seo / monitor / social / script / automation / scanner / empty / other）；
     * 非爬虫返回 {@code null}。用来把「抓取压力来自哪一类」聚成一张饼。
     */
    public static String botKind(String ua) {
        Rule rule = match(ua);
        return rule == null ? null : rule.kind;
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
