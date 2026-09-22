package com.site.blog.my.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分页查询参数
 *
 */
public class PageQueryUtil extends LinkedHashMap<String, Object> {
    //默认每页条数
    private static final int DEFAULT_LIMIT = 10;
    //每页条数上限
    private static final int MAX_LIMIT = 1000;

    //当前页码
    private int page;
    //每页条数
    private int limit;

    public PageQueryUtil(Map<String, Object> params) {
        this.putAll(params);

        //分页参数（容错处理，避免非法参数导致 500）
        try {
            this.page = Integer.parseInt(params.get("page").toString());
        } catch (Exception e) {
            this.page = 1;
        }
        try {
            this.limit = Integer.parseInt(params.get("limit").toString());
        } catch (Exception e) {
            this.limit = DEFAULT_LIMIT;
        }
        if (page < 1) page = 1;
        // 注意：超上限时按上限截断，而不是重置成默认值。
        //   原实现为 `limit = 10`，会让「取全部已发布文章」的场景（sitemap 传 1000、
        //   预渲染清单传 10000）静默退化成长度 10 —— 表现为 sitemap 只提交 10 篇文章、
        //   预渲染只覆盖最新 10 篇，且没有任何日志或异常，排查成本极高。
        if (limit < 1) {
            limit = DEFAULT_LIMIT;
        } else if (limit > MAX_LIMIT) {
            limit = MAX_LIMIT;
        }
        this.put("start", (page - 1) * limit);
        this.put("page", page);
        this.put("limit", limit);
    }


    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "PageUtil{" +
                "page=" + page +
                ", limit=" + limit +
                '}';
    }
}
