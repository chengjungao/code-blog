package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.Blog;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 静态页面渲染服务
 * 调用 Puppeteer 脚本将博客页面渲染为静态 HTML
 */
@Service
public class StaticRenderService {

    private static final Logger logger = LoggerFactory.getLogger(StaticRenderService.class);

    @Value("${blog.base-url:https://www.chengjungao.cn}")
    private String baseUrl;

    @Value("${renderer.script-path:/opt/app/renderer/render.js}")
    private String rendererScriptPath;

    @Value("${renderer.node-path:node}")
    private String nodePath;

    @Autowired
    private BlogService blogService;

    // 渲染锁，防止并发渲染
    private final AtomicBoolean rendering = new AtomicBoolean(false);

    /**
     * 渲染所有已发布博客
     * @return 渲染结果
     */
    public RenderResult renderAllBlogs() {
        if (!rendering.compareAndSet(false, true)) {
            return new RenderResult(false, "渲染任务正在执行中，请稍后再试");
        }

        try {
            List<String> urls = getAllBlogUrls();
            if (urls.isEmpty()) {
                return new RenderResult(true, "没有需要渲染的博客");
            }

            logger.info("开始渲染 {} 个博客页面", urls.size());
            return executeRender(urls);

        } catch (Exception e) {
            logger.error("渲染博客失败", e);
            return new RenderResult(false, "渲染失败: " + e.getMessage());
        } finally {
            rendering.set(false);
        }
    }

    /**
     * 渲染单个博客
     * @param blogId 博客 ID
     * @return 渲染结果
     */
    public RenderResult renderBlog(Long blogId) {
        if (!rendering.compareAndSet(false, true)) {
            return new RenderResult(false, "渲染任务正在执行中，请稍后再试");
        }

        try {
            Blog blog = blogService.getBlogById(blogId);
            if (blog == null || blog.getBlogStatus() != 1) {
                return new RenderResult(false, "博客不存在或未发布");
            }

            String url = getBlogUrl(blog);
            logger.info("渲染博客: {} -> {}", blogId, url);
            return executeRender(Collections.singletonList(url));

        } catch (Exception e) {
            logger.error("渲染博客 {} 失败", blogId, e);
            return new RenderResult(false, "渲染失败: " + e.getMessage());
        } finally {
            rendering.set(false);
        }
    }

    /**
     * 获取所有已发布博客的 URL
     */
    public List<String> getAllBlogUrls() {
        List<String> urls = new ArrayList<>();

        // 获取所有已发布博客
        HashMap<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("limit", 10000); // 获取所有
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        PageResult pageResult = blogService.getBlogsPage(pageUtil);
        
        @SuppressWarnings("unchecked")
        List<Blog> blogs = (List<Blog>) pageResult.getList();
        
        for (Blog blog : blogs) {
            if (blog.getBlogStatus() != null && blog.getBlogStatus() == 1) {
                urls.add(getBlogUrl(blog));
            }
        }

        // 添加首页
        urls.add(baseUrl + "/");
        // 添加笔记列表页
        urls.add(baseUrl + "/notes");
        // 添加生活杂记页
        urls.add(baseUrl + "/life");

        return urls;
    }

    /**
     * 获取博客的 URL（优先使用自定义路径）
     */
    private String getBlogUrl(Blog blog) {
        if (blog.getBlogSubUrl() != null && !blog.getBlogSubUrl().trim().isEmpty()) {
            return baseUrl + "/" + blog.getBlogSubUrl().trim();
        }
        return baseUrl + "/notes/" + blog.getBlogId();
    }

    /**
     * 执行渲染
     */
    private RenderResult executeRender(List<String> urls) {
        try {
            // 检查渲染脚本是否存在
            File scriptFile = new File(rendererScriptPath);
            if (!scriptFile.exists()) {
                return new RenderResult(false, "渲染脚本不存在: " + rendererScriptPath);
            }

            // 构建命令
            List<String> command = new ArrayList<>();
            command.add(nodePath);
            command.add(rendererScriptPath);
            command.addAll(urls);

            logger.debug("执行命令: {}", String.join(" ", command));

            // 执行渲染脚本
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            
            // 设置环境变量
            pb.environment().put("STATIC_PAGES_DIR", "/usr/share/nginx/html/blog/pages");

            Process process = pb.start();

            // 读取输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.info("[Renderer] {}", line);
                }
            }

            // 等待完成
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return new RenderResult(true, "渲染成功", output.toString());
            } else {
                return new RenderResult(false, "渲染失败，退出码: " + exitCode, output.toString());
            }

        } catch (Exception e) {
            logger.error("执行渲染脚本失败", e);
            return new RenderResult(false, "执行渲染脚本失败: " + e.getMessage());
        }
    }

    /**
     * 是否正在渲染
     */
    public boolean isRendering() {
        return rendering.get();
    }

    /**
     * 渲染结果
     */
    public static class RenderResult {
        private final boolean success;
        private final String message;
        private final String output;

        public RenderResult(boolean success, String message) {
            this(success, message, null);
        }

        public RenderResult(boolean success, String message, String output) {
            this.success = success;
            this.message = message;
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public String getOutput() {
            return output;
        }
    }
}
