package com.site.blog.my.core.entity;

/**
 * 访问明细事件（一行 = 一条 JSON Lines 日志记录）。
 *
 * <p>字段分两组：
 * <ul>
 *   <li>前端上报（浏览器侧）：type / path / title / ref / dur / vid / sid / scr / lang / ts</li>
 *   <li>服务端补全（可信侧）：st / ip / ipSrc / xff / ua / browser / os / device / internal</li>
 * </ul>
 *
 * <p>时间段（按小时统计）以服务端时间 {@code st} 为准，避免客户端时钟不可信。
 */
public class PageStatEvent {

    /** 事件类型：pv（页面浏览）、leave（离开，带停留时长） */
    private String type;

    /** 页面路径（已归一化：去掉 query/hash，最长 200） */
    private String path;

    /** 页面标题 */
    private String title;

    /** 来源页（只有落地页有值，站内跳转不带，避免把站内跳转算成外链） */
    private String ref;

    /** 停留秒数（leave 事件） */
    private Long dur;

    /** 访客 ID（localStorage，跨会话稳定） */
    private String vid;

    /** 会话 ID（sessionStorage，30 分钟滑动） */
    private String sid;

    /** 屏幕尺寸，如 1920x1080 */
    private String scr;

    /** 浏览器语言 */
    private String lang;

    /** 客户端时间（ISO8601） */
    private String ts;

    /** 服务端接收时间 yyyy-MM-dd HH:mm:ss */
    private String st;

    /** 解析出的客户端 IP */
    private String ip;

    /** IP 取值来源：xff / xff-private / realip / remote */
    private String ipSrc;

    /** 原始 X-Forwarded-For（核对 CDN 行为用，不参与统计） */
    private String xff;

    /** User-Agent（截断 400 字符） */
    private String ua;

    private String browser;

    private String os;

    private String device;

    /** 内网/本机来源（预渲染、内网直连）：写入明细但不计入报表 */
    private boolean internal;

    /** 爬虫/自动化（Googlebot 会执行 JS，只能靠 UA 拦）：写入明细但不计入报表 */
    private boolean bot;

    /** 是否为本次会话的落地页（ref 只在这一条上有意义） */
    private boolean entry;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public Long getDur() {
        return dur;
    }

    public void setDur(Long dur) {
        this.dur = dur;
    }

    public String getVid() {
        return vid;
    }

    public void setVid(String vid) {
        this.vid = vid;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getScr() {
        return scr;
    }

    public void setScr(String scr) {
        this.scr = scr;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public String getSt() {
        return st;
    }

    public void setSt(String st) {
        this.st = st;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIpSrc() {
        return ipSrc;
    }

    public void setIpSrc(String ipSrc) {
        this.ipSrc = ipSrc;
    }

    public String getXff() {
        return xff;
    }

    public void setXff(String xff) {
        this.xff = xff;
    }

    public String getUa() {
        return ua;
    }

    public void setUa(String ua) {
        this.ua = ua;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public boolean isBot() {
        return bot;
    }

    public void setBot(boolean bot) {
        this.bot = bot;
    }

    public boolean isEntry() {
        return entry;
    }

    public void setEntry(boolean entry) {
        this.entry = entry;
    }
}
