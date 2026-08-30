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
     * 只收录有实质内容的页面，不包含分类/标签等薄内容页
     */
    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    public String sitemap(HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        // 获取所有已发布博客，计算最新更新时间作为栏目页的 lastmod
        List<Blog> allBlogs = getAllPublishedBlogs();
        String latestUpdate = getLatestUpdateDate(allBlogs, dateFormat);
        
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        
        // 首页（有内容，带 lastmod）
        appendUrl(xml, baseUrl + "/", latestUpdate, "1.0");
        
        // 笔记列表页
        appendUrl(xml, baseUrl + "/notes", latestUpdate, "0.9");
        
        // 生活杂记页（取生活分类下最新文章时间）
        String lifeUpdate = getLatestDateByCategory(allBlogs, "生活", dateFormat);
        appendUrl(xml, baseUrl + "/life", lifeUpdate != null ? lifeUpdate : latestUpdate, "0.8");
        
        // 分类列表页（实际有内容的聚合页）
        appendUrl(xml, baseUrl + "/categories", latestUpdate, "0.7");
        
        // 作品集（高价值转化页）
        appendUrl(xml, baseUrl + "/portfolio", null, "0.8");
        
        // 所有已发布博客（优先使用自定义路径，带 lastmod）
        for (Blog blog : allBlogs) {
            String blogUrl = getBlogUrl(baseUrl, blog);
            String lastMod = blog.getUpdateTime() != null ? dateFormat.format(blog.getUpdateTime())
                                : (blog.getCreateTime() != null ? dateFormat.format(blog.getCreateTime()) : null);
            appendUrl(xml, blogUrl, lastMod, "0.8");
        }
        
        xml.append("</urlset>");
        return xml.toString();
    }

    /**
     * 添加一个 sitemap URL 条目
     */
    private void appendUrl(StringBuilder xml, String loc, String lastmod, String priority) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(loc).append("</loc>\n");
        if (lastmod != null && !lastmod.isEmpty()) {
            xml.append("    <lastmod>").append(lastmod).append("</lastmod>\n");
        }
        xml.append("    <priority>").append(priority).append("</priority>\n");
        xml.append("  </url>\n");
    }

    /**
     * 获取所有博客中最新的更新日期（作为栏目页的 lastmod）
     */
    private String getLatestUpdateDate(List<Blog> blogs, SimpleDateFormat dateFormat) {
        Date latest = null;
        for (Blog blog : blogs) {
            Date d = blog.getUpdateTime() != null ? blog.getUpdateTime() : blog.getCreateTime();
            if (d != null && (latest == null || d.after(latest))) {
                latest = d;
            }
        }
        return latest != null ? dateFormat.format(latest) : null;
    }

    /**
     * 获取指定分类下最新的更新日期（用于 /life 页面的 lastmod）
     */
    private String getLatestDateByCategory(List<Blog> blogs, String categoryName, SimpleDateFormat dateFormat) {
        Date latest = null;
        for (Blog blog : blogs) {
            if (categoryName.equals(blog.getBlogCategoryName())) {
                Date d = blog.getUpdateTime() != null ? blog.getUpdateTime() : blog.getCreateTime();
                if (d != null && (latest == null || d.after(latest))) {
                    latest = d;
                }
            }
        }
        return latest != null ? dateFormat.format(latest) : null;
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
        txt.append("- [分类列表](").append(baseUrl).append("/categories) - 按分类浏览\n");
        txt.append("- [作品集](").append(baseUrl).append("/portfolio) - 项目与作品\n\n");
        
        txt.append("## 最新文章\n\n");
        List<Blog> allBlogs = getAllPublishedBlogs();
        int count = 0;
        for (Blog blog : allBlogs) {
            if (count >= 20) break;
            String blogUrl = getBlogUrl(baseUrl, blog);
            txt.append("- [").append(blog.getBlogTitle()).append("](").append(blogUrl).append(")\n");
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
               .append(encodeUrl(category.getCategoryName())).append(")\n");
        }
        
        txt.append("\n## 标签\n\n");
        List<BlogTagCount> tags = tagService.getBlogTagCountForIndex();
        for (BlogTagCount tag : tags) {
            txt.append("- [").append(tag.getTagName()).append("](")
               .append(baseUrl).append("/tag/")
               .append(encodeUrl(tag.getTagName())).append(")\n");
        }
        
        return txt.toString();
    }

    /**
     * 获取基础 URL（支持反向代理）
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        
        // 优先使用反向代理头（Nginx 会设置 X-Forwarded-Proto）
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        
        boolean behindProxy = false;
        if (forwardedProto != null) {
            scheme = forwardedProto;
            behindProxy = true;
        }
        if (forwardedHost != null) {
            serverName = forwardedHost.split(":")[0];
        }
        
        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);
        
        // 反向代理模式下不附加端口（代理已处理端口转发）
        if (!behindProxy) {
            int serverPort = request.getServerPort();
            if ((scheme.equals("http") && serverPort != 80) || 
                (scheme.equals("https") && serverPort != 443)) {
                baseUrl.append(":").append(serverPort);
            }
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
