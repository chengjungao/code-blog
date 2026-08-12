package com.site.blog.my.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 分页查询参数
 *
 */
public class PageQueryUtil extends LinkedHashMap<String, Object> {
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
            this.limit = 10;
        }
        if (page < 1) page = 1;
        if (limit < 1 || limit > 100) limit = 10;
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
