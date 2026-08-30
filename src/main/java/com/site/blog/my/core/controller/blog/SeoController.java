package com.site.blog.my.core.controller.blog;

import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.entity.BlogCategory;
import com.site.blog.my.core.entity.BlogTagCount;
import com.site.blog.my.core.service.BlogService;
import com.site.blog.my.core.service.CategoryService;
import com.site.blog.my.core.service.TagService;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * SEO 相关文件接口
 */
@RestController
public class SeoController {

    @Autowired
    private BlogService blogService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private TagService tagService;

    /**
     * 生成 sitemap.xml
     */
    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    public String sitemap(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // 首页
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>1.0</priority>\n");
        xml.append("  </url>\n");
        
        // 笔记列表页
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/notes</loc>\n");
        xml.append("    <changefreq>daily</changefreq>\n");
        xml.append("    <priority>0.9</priority>\n");
        xml.append("  </url>\n");
        
        // 生活杂记页
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/life</loc>\n");
        xml.append("    <changefreq>weekly</changefreq>\n");
        xml.append("    <priority>0.8</priority>\n");
        xml.append("  </url>\n");
        
        // 分类页
        xml.append("  <url>\n");
        xml.append("    <loc>").append(baseUrl).append("/categories</loc>\n");
        xml.append("    <changefreq>weekly</changefreq>\n");
        xml.append("    <priority>0.7</priority>\n");
        xml.append("  </url>\n");
        
        // 所有已发布博客
        List<Blog> allBlogs = getAllPublishedBlogs();
        for (Blog blog : allBlogs) {
            String blogUrl = getBlogUrl(baseUrl, blog);
            String lastMod = blog.getUpdateTime() != null ? dateFormat.format(blog.getUpdateTime()) 
                                : (blog.getCreateTime() != null ? dateFormat.format(blog.getCreateTime()) : "");
            
            xml.append("  <url>\n");
            xml.append("    <loc>").append(blogUrl).append("</loc>\n");
            if (!lastMod.isEmpty()) {
                xml.append("    <lastmod>").append(lastMod).append("</lastmod>\n");
            }
            xml.append("    <changefreq>monthly</changefreq>\n");
            xml.append("    <priority>0.8</priority>\n");
            xml.append("  </url>\n");
        }
        
        // 所有分类
        List<BlogCategory> categories = categoryService.getAllCategories();
        for (BlogCategory category : categories) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/category/")
               .append(encodeUrl(category.getCategoryName())).append("/1</loc>\n");
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.6</priority>\n");
            xml.append("  </url>\n");
        }
        
        // 所有标签
        List<BlogTagCount> tags = tagService.getBlogTagCountForIndex();
        for (BlogTagCount tag : tags) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(baseUrl).append("/tag/")
               .append(encodeUrl(tag.getTagName())).append("/1</loc>\n");
            xml.append("    <changefreq>weekly</changefreq>\n");
            xml.append("    <priority>0.5</priority>\n");
            xml.append("  </url>\n");
        }
        
        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * 生成 llms.txt - LLM 友好的内容索引
     */
    @GetMapping(value = "/llms.txt", produces = "text/plain;charset=UTF-8")
    public String llmsTxt(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        
        StringBuilder txt = new StringBuilder();
        txt.append("# 程军高的技术博客\n\n");
        txt.append("> 搜索、AI、架构、工程实践——写下来才真正属于自己的。\n\n");
        txt.append("## 网站信息\n\n");
        txt.append("- 网站地址: ").append(baseUrl).append("\n");
        txt.append("- 主要内容: 技术笔记、生活杂记\n");
        txt.append("- 主题领域: 搜索技术、人工智能、软件架构、工程实践\n\n");
        
        txt.append("## 主要栏目\n\n");
        txt.append("- [技术笔记](").append(baseUrl).append("/notes) - 技术文章和教程\n");
        txt.append("- [生活杂记](").append(baseUrl).append("/life) - 读书心得、做菜笔记\n");
        txt.append("- [分类列表](").append(baseUrl).append("/categories) - 按分类浏览\n\n");
        
        txt.append("## 最新文章\n\n");
        List<Blog> allBlogs = getAllPublishedBlogs();
        int count = 0;
        for (Blog blog : allBlogs) {
            if (count >= 20) break; // 只列出最新 20 篇
            String blogUrl = getBlogUrl(baseUrl, blog);
            txt.append("- [").append(blog.getBlogTitle()).append("](").append(blogUrl).append(")\n");
            if (blog.getBlogSubUrl() != null && !blog.getBlogSubUrl().isEmpty()) {
                txt.append("  - 自定义路径: /").append(blog.getBlogSubUrl()).append("\n");
            }
            if (blog.getBlogCategoryName() != null) {
                txt.append("  - 分类: ").append(blog.getBlogCategoryName()).append("\n");
            }
            if (blog.getBlogTags() != null && !blog.getBlogTags().isEmpty()) {
                txt.append("  - 标签: ").append(blog.getBlogTags()).append("\n");
            }
            txt.append("\n");
            count++;
        }
        
        txt.append("## 分类\n\n");
        List<BlogCategory> categories = categoryService.getAllCategories();
        for (BlogCategory category : categories) {
            txt.append("- [").append(category.getCategoryName()).append("](")
               .append(baseUrl).append("/category/")
               .append(encodeUrl(category.getCategoryName())).append("/1)\n");
        }
        
        txt.append("\n## 标签\n\n");
        List<BlogTagCount> tags = tagService.getBlogTagCountForIndex();
        for (BlogTagCount tag : tags) {
            txt.append("- [").append(tag.getTagName()).append("](")
               .append(baseUrl).append("/tag/")
               .append(encodeUrl(tag.getTagName())).append("/1)\n");
        }
        
        return txt.toString();
    }

    /**
     * 获取基础 URL
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        
        // 优先使用请求头中的信息（支持反向代理）
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        
        if (forwardedProto != null) {
            scheme = forwardedProto;
        }
        if (forwardedHost != null) {
            serverName = forwardedHost.split(":")[0];
            serverPort = forwardedHost.contains(":") ? Integer.parseInt(forwardedHost.split(":")[1]) : 443;
        }
        
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        
        if ((scheme.equals("http") && serverPort != 80) || 
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }
        
        return baseUrl.toString();
    }

    /**
     * 获取博客 URL（优先使用自定义路径）
     */
    private String getBlogUrl(String baseUrl, Blog blog) {
        if (blog.getBlogSubUrl() != null && !blog.getBlogSubUrl().trim().isEmpty()) {
            return baseUrl + "/" + blog.getBlogSubUrl().trim();
        }
        return baseUrl + "/notes/" + blog.getBlogId();
    }

    /**
     * URL 编码
     */
    private String encodeUrl(String str) {
        try {
            return java.net.URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return str;
        }
    }

    /**
     * 获取所有已发布博客
     */
    private List<Blog> getAllPublishedBlogs() {
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("limit", 1000); // 获取所有
        params.put("blogStatus", 1); // 已发布
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        PageResult pageResult = blogService.getBlogsPage(pageUtil);
        return (List<Blog>) pageResult.getList();
    }
}
