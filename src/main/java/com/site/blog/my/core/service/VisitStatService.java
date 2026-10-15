package com.site.blog.my.core.service;

import java.util.Map;

public interface VisitStatService {

    /**
     * 记录一次页面访问（实时数据只进内存，定时聚合后写 DB）。
     * <p>注意：正式埋点链路已改为「有界内存队列 + 单消费者线程写明细文件」，
     * 由 {@code PageStatLogService} 在消费者线程上调用本方法。
     * 保留此入口是为了兼容旧调用与本地联调。
     *
     * @param pagePath 页面路径
     * @param ip       访问者 IP（用于 UV 去重）
     */
    void record(String pagePath, String ip);

    /**
     * 查询图表数据：每日 PV/UV 趋势 + TOP 页面 + 总量（旧口径，直接读 DB）
     *
     * @param startDate yyyy-MM-dd
     * @param endDate   yyyy-MM-dd
     */
    Map<String, Object> getStatOverview(String startDate, String endDate);

    /**
     * 多维报表：读访问明细（JSONL）聚合，缺失区间自动回退 DB 汇总。
     *
     * <p>输出维度：summary / trend / hourly / pages / sources / devices /
     * browsers / systems / durations / health / realtime，并保留旧字段
     * daily / topPages / total 以兼容既有调用。
     *
     * @param startDate yyyy-MM-dd
     * @param endDate   yyyy-MM-dd
     */
    Map<String, Object> getStatReport(String startDate, String endDate);

    /**
     * 今日实时概览（内存计数，不等 5 分钟聚合）
     */
    Map<String, Object> getRealtime();

    /**
     * 定时聚合：把内存中的实时计数刷入 DB
     */
    void flush();
}
