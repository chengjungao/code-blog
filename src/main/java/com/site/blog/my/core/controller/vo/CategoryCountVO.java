package com.site.blog.my.core.controller.vo;

import java.io.Serializable;

/**
 * 列表页类目筛选条用的分类计数（只统计已发布文章）
 */
public class CategoryCountVO implements Serializable {

    private String categoryName;

    private Integer categoryCount;

    public CategoryCountVO() {
    }

    public CategoryCountVO(String categoryName, Integer categoryCount) {
        this.categoryName = categoryName;
        this.categoryCount = categoryCount;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getCategoryCount() {
        return categoryCount;
    }

    public void setCategoryCount(Integer categoryCount) {
        this.categoryCount = categoryCount;
    }
}
