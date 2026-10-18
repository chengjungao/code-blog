package com.site.blog.my.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.site.blog.my.core.entity.PageStatEvent;
import com.site.blog.my.core.service.PageStatCounters;
import com.site.blog.my.core.service.PageStatLogService;
import com.site.blog.my.core.util.UserAgentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 访问明细落盘：有界内存队列 + 单消费者线程。
 *
 * <p>职责边界：<b>只负责「安全地收下」和「顺序写进文件」</b>，不做任何报表计算。
 * 报表聚合由 {@link VisitStatServiceImpl#getStatReport} 读文件完成，两边解耦。
 *
 * <p>写盘线程在批量处理时才做这些「重活」：UA 解析、路径校验、爬虫识别。
 * 请求线程只做「取出字段 → 塞进队列」，这是性能与可靠性同时成立的关键。
 */
@Service
public class PageStatLogServiceImpl implements PageStatLogService {

    private static final Logger LOG = LoggerFactory.getLogger(PageStatLogServiceImpl.class);

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String FILE_PREFIX = "stat-";
    private static final String FILE_SUFFIX = ".jsonl";

    /** 明细行的字段长度上限（防御脏数据把日志文件撑爆） */
    private static final int MAX_PATH = 200;
    private static final int MAX_REF = 300;
    private static final int MAX_UA = 400;
    private static final int MAX_ID = 64;
    private static final int MAX_TITLE = 120;

    /** 不统计的路径前缀 / 静态资源后缀（前端理论上只发路由 path，这里是第二道闸） */
    private static final Pattern SKIP_PATH = Pattern.compile(
            "^/(admin|blog/api|common|api|assets|upload|_validate_path)(/|$)"
                    + "|^/[^/]*\\.(js|css|map|png|jpe?g|gif|svg|ico|webp|woff2?|ttf|eot|json|xml|txt)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * 只拦控制字符。
     *
     * <p>⚠️ 这里必须用「黑名单」而不是 {@code \p{Print}} 之类的白名单：
     * Java 默认模式下 {@code \p{Print}} 只覆盖 ASCII，中文路径
     * （如 {@code /category/工程化/1}）会被判为非法而全部丢弃——
     * 对一个中文站点来说，这等于把半站流量扔掉，而且失败得很安静。
     * 路径只写进 JSONL（会被 JSON 转义），从不当作文件路径，因此放开非控制字符是安全的。
     */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\x00-\\x1f\\x7f]");

    private volatile BlockingQueue<PageStatEvent> queue;

    @Value("${page-stat.log-dir:data/page-stat}")
    private String logDir;

    @Value("${page-stat.queue-capacity:2000}")
    private int capacity;

    @Value("${page-stat.batch-size:500}")
    private int batchSize;

    @Value("${page-stat.retain-days:90}")
    private int retainDays;

    /** full：明细里保留完整 IP（便于核对 CDN 链路）；mask：只留网段（隐私优先） */
    @Value("${page-stat.ip-mode:full}")
    private String ipMode;

    @Value("${page-stat.enabled:true}")
    private boolean enabled;

    @Autowired
    private PageStatCounters counters;

    // ---- 运行指标（报表「数据健康」卡片直接读这些值） ----
    private final AtomicLong received = new AtomicLong();
    private final AtomicLong written = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong bots = new AtomicLong();
    private final AtomicLong internals = new AtomicLong();

    private volatile long lastWriteMillis = 0L;
    private volatile String lastError = "";
    private volatile String currentFile = "";

    private volatile boolean running = false;
    private volatile Thread worker;
    private BufferedWriter writer;
    private String writerDay = "";

    public PageStatLogServiceImpl() {
        // 先给一个默认容量，@PostConstruct 里按配置重建
        this.queue = new ArrayBlockingQueue<PageStatEvent>(2000);
    }

    // ==================== 生命周期 ====================

    @PostConstruct
    public void start() {
        if (!enabled) {
            LOG.info("[stat] 访问明细落盘已关闭（page-stat.enabled=false）");
            return;
        }
        if (capacity < 100) {
            capacity = 100;
        }
        if (capacity > 100000) {
            capacity = 100000;
        }
        // 有界队列：容量故意做小。它只是「削峰缓冲」，不是数据仓库——
        // 真要有 2000 条积压，说明下游写盘已经出问题，此时丢弃比堆积更安全。
        this.queue = new ArrayBlockingQueue<PageStatEvent>(capacity);
        if (batchSize < 1 || batchSize > capacity) {
            batchSize = Math.min(500, capacity);
        }
        try {
            Path dir = Paths.get(logDir);
            Files.createDirectories(dir);
            LOG.info("[stat] 明细目录 = {}（队列容量 {}，批量 {}，保留 {} 天，IP 模式 {}）",
                    dir.toAbsolutePath(), capacity, batchSize, retainDays, ipMode);
        } catch (IOException e) {
            lastError = "创建目录失败: " + e.getMessage();
            LOG.error("[stat] 无法创建明细目录 {}，埋点将退化为丢弃模式", logDir, e);
        }
        cleanExpired();
        running = true;
        worker = new Thread(this::consumeLoop, "page-stat-consumer");
        worker.setDaemon(true);
        worker.start();
    }

    @PreDestroy
    public void stop() {
        running = false;
        Thread t = worker;
        if (t != null) {
            t.interrupt();
            try {
                // 给消费者 5 秒把积压写完，牺牲这点停机时间换数据不丢
                t.join(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        synchronized (this) {
            closeWriter();
        }
        LOG.info("[stat] 明细线程已停止：received={} written={} dropped={}",
                received.get(), written.get(), dropped.get());
    }

    // ==================== 写入侧：请求线程 ====================

    @Override
    public boolean offer(PageStatEvent event) {
        if (!enabled || event == null) {
            return false;
        }
        // 关键：offer 不阻塞。队列满 → 直接丢，请求线程立刻返回，绝不排队等待。
        if (!queue.offer(event)) {
            dropped.incrementAndGet();
            return false;
        }
        received.incrementAndGet();
        return true;
    }

    // ==================== 消费侧：单线程 ====================

    private void consumeLoop() {
        List<PageStatEvent> batch = new ArrayList<PageStatEvent>(Math.max(64, batchSize));
        while (running) {
            try {
                PageStatEvent first = queue.poll(500, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                batch.clear();
                batch.add(first);
                queue.drainTo(batch, batchSize - 1);
                handleBatch(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Throwable t) {
                // 消费者绝不能因为单条脏数据而死掉，否则队列会静默填满
                lastError = STAMP.format(LocalDateTime.now()) + " " + t;
                LOG.warn("[stat] 写入批次异常，已跳过：{}", t.toString());
            }
        }
        // 停机前把队列里剩下的尽量写完
        drainRemaining(batch);
    }

    private void drainRemaining(List<PageStatEvent> batch) {
        try {
            while (!queue.isEmpty()) {
                batch.clear();
                queue.drainTo(batch, Math.max(64, batchSize));
                if (batch.isEmpty()) {
                    break;
                }
                handleBatch(batch);
            }
            synchronized (this) {
                if (writer != null) {
                    writer.flush();
                }
            }
        } catch (Throwable t) {
            LOG.warn("[stat] 停机清空队列时出错：{}", t.toString());
        }
    }

    /** 一个批次：先补全字段（UA/爬虫/路径校验），再统一写盘，最后更新内存聚合 */
    private void handleBatch(List<PageStatEvent> batch) throws IOException {
        List<PageStatEvent> toAggregate = new ArrayList<PageStatEvent>(batch.size());
        for (PageStatEvent e : batch) {
            enrich(e);
            if (e.getPath() == null) {
                rejected.incrementAndGet();
                continue;
            }
            writeLine(e);
            // 内网优先判：预渲染来自回环地址、UA 又是 HeadlessChrome，
            // 两个标记会同时成立。若先按 bot 归类，每天几十次预渲染就会冒充成「爬虫抓取」。
            if (e.isInternal()) {
                internals.incrementAndGet();
            } else if (e.isBot()) {
                bots.incrementAndGet();
            } else if ("leave".equals(e.getType())) {
                // 停留事件只进明细，不是一次页面浏览。
                // 漏这一步的代价实测过：切后台/关页面都会各发一次 leave，
                // tb_page_stat 的 PV 会比明细多出 30%~80%（10-18 实测 DB 44 vs 明细 23），
                // 于是「DB 兜底的老日期」和「明细口径的新日期」永远对不上。
            } else {
                toAggregate.add(e);
            }
        }
        synchronized (this) {
            if (writer != null) {
                writer.flush();
            }
        }
        lastWriteMillis = System.currentTimeMillis();
        // 只把「有效访问」喂给内存计数，爬虫/内网流量已在上面分流，
        // 后续 5 分钟落 DB 的链路完全不变。
        for (PageStatEvent e : toAggregate) {
            try {
                counters.record(e.getPath(), e.getIp());
            } catch (Throwable t) {
                LOG.warn("[stat] 内存聚合失败：{}", t.toString());
            }
        }
        written.addAndGet(batch.size());
    }

    /** 补全服务端字段 + 清洗。这里是唯一做「重活」的地方，不在请求线程上。 */
    private void enrich(PageStatEvent e) {
        String ua = clip(e.getUa(), MAX_UA);
        e.setUa(ua);
        // 标识与判定同源：能把「是不是爬虫」和「是哪个爬虫」一次说清，不会出现判定与标识打架
        String botName = UserAgentParser.botName(ua);
        e.setBot(botName != null);
        if (botName != null) {
            e.setBotName(botName);
            e.setBotKind(UserAgentParser.botKind(ua));
        } else {
            e.setBotName(null);
            e.setBotKind(null);
        }
        e.setBrowser(UserAgentParser.browser(ua));
        e.setOs(UserAgentParser.os(ua));
        e.setDevice(UserAgentParser.device(ua));

        e.setPath(normalizePath(e.getPath()));
        e.setRef(normalizeRef(e.getRef()));
        e.setTitle(clip(safe(e.getTitle()), MAX_TITLE));
        e.setVid(clip(safe(e.getVid()), MAX_ID));
        e.setSid(clip(safe(e.getSid()), MAX_ID));
        e.setScr(clip(safe(e.getScr()), 20));
        e.setLang(clip(safe(e.getLang()), 20));
        e.setTs(clip(safe(e.getTs()), 32));
        if (e.getType() == null || e.getType().isEmpty()) {
            e.setType("pv");
        }
        if (e.getDur() != null && (e.getDur() < 0 || e.getDur() > 86400)) {
            e.setDur(null);
        }
        if (e.getSt() == null) {
            e.setSt(STAMP.format(LocalDateTime.now()));
        }
        if ("mask".equalsIgnoreCase(ipMode)) {
            e.setIp(maskIp(e.getIp()));
            e.setXff(maskChain(e.getXff()));
        }
    }

    /** 路径归一化：去 query/hash、去重复斜杠、去掉末尾斜杠；不合法返回 null（该条丢弃） */
    public static String normalizePath(String raw) {
        if (raw == null) {
            return null;
        }
        String p = raw.trim();
        int cut = p.indexOf('#');
        if (cut >= 0) {
            p = p.substring(0, cut);
        }
        cut = p.indexOf('?');
        if (cut >= 0) {
            p = p.substring(0, cut);
        }
        while (p.indexOf("//") >= 0) {
            p = p.replace("//", "/");
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        if (p.isEmpty() || p.charAt(0) != '/' || p.length() > MAX_PATH) {
            return null;
        }
        if (p.contains("..") || CONTROL_CHARS.matcher(p).find()) {
            return null;
        }
        if (SKIP_PATH.matcher(p).find()) {
            return null;
        }
        return p;
    }

    /**
     * 来源页归一化：剥掉协议、query、hash，只留 host+path。
     * <p>必须去掉 query——搜索引擎的 referrer 里带着用户搜的关键词，
     * 那是实打实的隐私，统计只需要知道「来自哪个站」。
     */
    public static String normalizeRef(String raw) {
        if (raw == null) {
            return null;
        }
        String s = safe(raw).trim();
        if (s.isEmpty()) {
            return null;
        }
        int i = s.indexOf("://");
        if (i >= 0) {
            s = s.substring(i + 3);
        }
        int cut = s.indexOf('#');
        if (cut >= 0) {
            s = s.substring(0, cut);
        }
        cut = s.indexOf('?');
        if (cut >= 0) {
            s = s.substring(0, cut);
        }
        if (s.isEmpty()) {
            return null;
        }
        return clip(s, MAX_REF);
    }

    private static String safe(String s) {
        return s == null ? "" : s.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** IPv4 → a.b.c.*；IPv6 → 保留前 3 组 */
    static String maskIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ip;
        }
        if (ip.indexOf(':') >= 0) {
            String[] g = ip.split(":");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(3, g.length); i++) {
                sb.append(g[i]).append(':');
            }
            return sb.append('*').toString();
        }
        int last = ip.lastIndexOf('.');
        return last > 0 ? ip.substring(0, last) + ".*" : ip;
    }

    static String maskChain(String chain) {
        if (chain == null || chain.isEmpty()) {
            return chain;
        }
        String[] parts = chain.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(maskIp(parts[i].trim()));
        }
        return sb.toString();
    }

    // ==================== 文件读写 ====================

    private void writeLine(PageStatEvent e) throws IOException {
        String day = e.getSt().length() >= 10 ? e.getSt().substring(0, 10) : LocalDate.now().toString();
        BufferedWriter w = writerFor(day);
        w.write(JSON.toJSONString(e));
        w.newLine();
        if (currentFile.isEmpty()) {
            currentFile = fileOf(day).getFileName().toString();
        }
    }

    private synchronized BufferedWriter writerFor(String day) throws IOException {
        if (writer != null && day.equals(writerDay)) {
            return writer;
        }
        closeWriter();
        Path f = fileOf(day);
        Files.createDirectories(f.getParent());
        writer = Files.newBufferedWriter(f, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        writerDay = day;
        currentFile = f.getFileName().toString();
        return writer;
    }

    private synchronized void closeWriter() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException ignored) {
                // 关闭失败无需处理，下次写入会重建
            }
            writer = null;
            writerDay = "";
        }
    }

    private Path fileOf(String day) {
        return Paths.get(logDir, FILE_PREFIX + day + FILE_SUFFIX);
    }

    @Override
    public void forEachEvent(LocalDate from, LocalDate to, Consumer<PageStatEvent> consumer) {
        if (from == null || to == null || consumer == null) {
            return;
        }
        Path dir = Paths.get(logDir);
        if (!Files.isDirectory(dir)) {
            return;
        }
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Path f = fileOf(d.toString());
            if (!Files.isRegularFile(f)) {
                continue;
            }
            try (Stream<String> lines = Files.lines(f, StandardCharsets.UTF_8)) {
                lines.forEach(line -> {
                    if (line.isEmpty()) {
                        return;
                    }
                    try {
                        PageStatEvent e = JSON.parseObject(line, PageStatEvent.class);
                        if (e != null) {
                            consumer.accept(e);
                        }
                    } catch (Exception ignored) {
                        // 单行损坏不影响其余明细
                    }
                });
            } catch (IOException e) {
                LOG.warn("[stat] 读取明细失败 {}：{}", f, e.toString());
            }
        }
    }

    @Override
    public List<String> files() {
        List<String> out = new ArrayList<String>();
        Path dir = Paths.get(logDir);
        if (!Files.isDirectory(dir)) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, FILE_PREFIX + "*" + FILE_SUFFIX)) {
            for (Path p : ds) {
                out.add(p.getFileName().toString());
            }
        } catch (IOException e) {
            LOG.warn("[stat] 列举明细文件失败：{}", e.toString());
        }
        Collections.sort(out, Comparator.reverseOrder());
        return out;
    }

    @Override
    public Map<String, Object> health() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("enabled", enabled);
        m.put("queueSize", queue.size());
        m.put("queueCapacity", queue.size() + queue.remainingCapacity());
        m.put("received", received.get());
        m.put("written", written.get());
        m.put("dropped", dropped.get());
        m.put("rejected", rejected.get());
        m.put("bot", bots.get());
        m.put("internal", internals.get());
        m.put("logDir", logDir);
        m.put("currentFile", currentFile);
        m.put("ipMode", ipMode);
        m.put("retainDays", retainDays);
        m.put("consumerAlive", worker != null && worker.isAlive());
        m.put("lastWrite", lastWriteMillis <= 0 ? "" : STAMP.format(
                java.time.Instant.ofEpochMilli(lastWriteMillis)
                        .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()));
        m.put("lastError", lastError);
        return m;
    }

    // ==================== 定时维护 ====================

    /**
     * 每小时一次：① 清理过期明细文件 ② 消费者看门狗。
     * 看门狗是必要的——消费者若因未知异常退出，队列会变成只进不出的黑洞，
     * 表现是「统计突然全是 0 且没有任何报错」，这里主动拉起来。
     */
    @Scheduled(fixedRate = 3600000L, initialDelay = 300000L)
    public void maintain() {
        if (!enabled) {
            return;
        }
        if (worker == null || !worker.isAlive()) {
            LOG.warn("[stat] 消费者线程已退出，正在重启");
            running = true;
            worker = new Thread(this::consumeLoop, "page-stat-consumer");
            worker.setDaemon(true);
            worker.start();
        }
        cleanExpired();
    }

    /** 保留期清理：按文件名里的日期判断，只删本服务自己产出的 stat-YYYY-MM-DD.jsonl */
    void cleanExpired() {
        Path dir = Paths.get(logDir);
        if (!Files.isDirectory(dir)) {
            return;
        }
        LocalDate deadline = LocalDate.now().minusDays(Math.max(1, retainDays));
        int removed = 0;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, FILE_PREFIX + "*" + FILE_SUFFIX)) {
            for (Path p : ds) {
                String name = p.getFileName().toString();
                String day = name.substring(FILE_PREFIX.length(), name.length() - FILE_SUFFIX.length());
                try {
                    if (LocalDate.parse(day, DAY).isBefore(deadline)) {
                        Files.deleteIfExists(p);
                        removed++;
                    }
                } catch (Exception ignored) {
                    // 文件名不合规的一律不动，避免误删
                }
            }
        } catch (IOException e) {
            LOG.warn("[stat] 清理过期明细失败：{}", e.toString());
        }
        if (removed > 0) {
            LOG.info("[stat] 已清理 {} 个过期明细文件（保留 {} 天）", removed, retainDays);
        }
    }
}
