package com.site.blog.my.core.service;

import java.util.List;
import java.util.Map;

public interface VisitStatService {

    /**
     * 记录一次页面访问（实时数据只进内存，定时聚合后写 DB）
     *
     * @param pagePath 页面路径
     * @param ip       访问者 IP（用于 UV 去重）
     */
    void record(String pagePath, String ip);

    /**
     * 查询图表数据：每日 PV/UV 趋势 + TOP 页面 + 总量
     *
     * @param startDate yyyy-MM-dd
     * @param endDate   yyyy-MM-dd
     */
    Map<String, Object> getStatOverview(String startDate, String endDate);

    /**
     * 定时聚合：把内存中的实时计数刷入 DB
     */
    void flush();
}
