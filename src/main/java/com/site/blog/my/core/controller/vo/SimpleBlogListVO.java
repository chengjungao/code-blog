package com.site.blog.my.core.controller.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 侧边栏精简文章项（最新发布 / 高频阅读）。
 * 字段由 BlogServiceImpl#getBlogListForIndexPage 的 BeanUtils.copyProperties 从 Blog 实体拷入，
 * 新增字段只要名字与实体一致即可自动填充。
 */
public class SimpleBlogListVO implements Serializable {

    private Long blogId;

    private String blogTitle;

    private String blogSubUrl;

    /** 发布日期，只到天：侧栏「最新发布」末尾展示 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createTime;

    /** 浏览次数，侧栏「高频阅读」末尾展示 */
    private Long blogViews;

    public Long getBlogId() {
        return blogId;
    }

    public void setBlogId(Long blogId) {
        this.blogId = blogId;
    }

    public String getBlogTitle() {
        return blogTitle;
    }

    public void setBlogTitle(String blogTitle) {
        this.blogTitle = blogTitle;
    }

    public String getBlogSubUrl() {
        return blogSubUrl;
    }

    public void setBlogSubUrl(String blogSubUrl) {
        this.blogSubUrl = blogSubUrl == null ? null : blogSubUrl.trim();
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Long getBlogViews() {
        return blogViews;
    }

    public void setBlogViews(Long blogViews) {
        this.blogViews = blogViews;
    }
}
