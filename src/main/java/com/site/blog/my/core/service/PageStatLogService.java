package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.PageStatEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 访问明细落盘服务。
 *
 * <p><b>为什么要有这一层</b>：埋点接口是「高 QPS、低价值、可丢失」的写请求。
 * 让它直接在 Tomcat 线程里写文件（或写 DB），一旦磁盘抖动、日志盘打满、锁竞争，
 * 请求就会堆积，反过来把整个站点的接口拖慢——埋点把主站拖挂，是最不划算的事故。
 *
 * <p>因此采用 <b>有界内存队列 + 单消费者线程</b>：
 * <pre>
 *   Tomcat 线程：offer(event) —— 非阻塞，纳秒级返回，满则丢弃并计数
 *   消费者线程：poll + drainTo 批量 → 解析/过滤/写 JSONL → 定期清理过期文件
 * </pre>
 * 请求路径上没有任何磁盘 IO、没有 synchronized、没有 DB 连接。
 * 队列满时的策略是 <b>丢弃 + 计数</b>（而不是阻塞等待）：
 * 埋点数据丢几条远好过拖慢真实用户请求，丢弃量会出现在报表「数据健康」里，不会静默。
 */
public interface PageStatLogService {

    /**
     * 非阻塞入队。返回 false 表示队列已满、该事件被丢弃（已计入 dropped）。
     * 调用方不需要做任何补偿处理，也<b>不应该</b>重试或阻塞。
     */
    boolean offer(PageStatEvent event);

    /**
     * 流式读取 [from, to] 区间的明细事件（按天分文件，逐行回调，不在内存里堆全量列表）。
     * 文件不存在（例如区间早于功能上线）直接跳过，由调用方回退到 DB 汇总。
     */
    void forEachEvent(LocalDate from, LocalDate to, Consumer<PageStatEvent> consumer);

    /**
     * 队列与写入链路的健康度：容量、积压、已接收/已写/已丢弃、当前文件、最近写入时间。
     * 报表页据此展示「数据准不准」，避免「静默丢数据」导致统计变成一个黑盒。
     */
    Map<String, Object> health();

    /**
     * 当前已有的明细文件名（倒序），用于排查与人工核对。
     */
    List<String> files();
}
