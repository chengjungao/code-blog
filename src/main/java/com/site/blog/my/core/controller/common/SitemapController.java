package com.site.blog.my.core.controller.common;

import com.site.blog.my.core.dao.BlogMapper;
import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogCategory;
import com.site.blog.my.core.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 动态生成 sitemap.xml，供 Google / Baidu 等搜索引擎抓取站点 URL。
 */
@RestController
public class SitemapController {

    @Value("${site.url:https://www.chengjungao.cn}")
    private String siteUrl;

    @Autowired
    private BlogMapper blogMapper;

    @Autowired
    private CategoryService categoryService;

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">");

        // 核心页面
        addUrl(sb, "/", "1.0", null);
        addUrl(sb, "/portfolio", "0.8", null);
        addUrl(sb, "/notes", "0.8", null);
        addUrl(sb, "/life", "0.7", null);
        addUrl(sb, "/categories", "0.6", null);

        // 文章详情页（已发布）
        List<Blog> blogs = blogMapper.findAllPublished();
        if (blogs != null) {
            for (Blog blog : blogs) {
                Date lastmod = blog.getUpdateTime() != null ? blog.getUpdateTime() : blog.getCreateTime();
                addUrl(sb, "/notes/" + blog.getBlogId(), "0.8", lastmod);
            }
        }

        // 分类页
        List<BlogCategory> categories = categoryService.getAllCategories();
        if (categories != null) {
            for (BlogCategory category : categories) {
                addUrl(sb, "/category/" + encodePath(category.getCategoryName()) + "/1", "0.6", null);
            }
        }

        sb.append("</urlset>");
        return sb.toString();
    }

    private String encodePath(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 必定支持，理论上不会走到这里
            return value;
        }
    }

    private void addUrl(StringBuilder sb, String path, String priority, Date lastmod) {
        sb.append("<url>");
        sb.append("<loc>").append(siteUrl).append(path).append("</loc>");
        if (lastmod != null) {
            sb.append("<lastmod>").append(new SimpleDateFormat("yyyy-MM-dd").format(lastmod)).append("</lastmod>");
        }
        sb.append("<changefreq>weekly</changefreq>");
        sb.append("<priority>").append(priority).append("</priority>");
        sb.append("</url>");
    }
}
