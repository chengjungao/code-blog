package com.site.blog.my.core.controller.blog;

import com.site.blog.my.core.service.BlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 路径验证接口 - 用于 Nginx 判断是否返回 404
 */
@RestController
@RequestMapping("/api")
public class PathValidationController {

    @Autowired
    private BlogService blogService;

    /**
     * 前端固定路由列表
     */
    private static final Set<String> VALID_ROUTES = new HashSet<>(Arrays.asList(
            "/",
            "/portfolio",
            "/notes",
            "/life",
            "/message",
            "/categories",
            "/link",
            "/about"
    ));

    /**
     * 静态资源扩展名
     */
    private static final Set<String> STATIC_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".css", ".js", ".png", ".jpg", ".jpeg", ".gif", ".svg", ".ico",
            ".woff", ".woff2", ".ttf", ".eot", ".json", ".xml", ".txt",
            ".mp4", ".webm", ".mp3", ".ogg", ".pdf", ".zip"
    ));

    /**
     * 验证路径是否有效（供 Nginx auth_request 使用）
     * @return 200 表示路径有效，403 表示应该返回 404
     */
    @GetMapping("/validate-path")
    public ResponseEntity<Void> validatePath(HttpServletRequest request) {
        // 从请求头获取原始路径
        String path = request.getHeader("X-Original-URI");
        if (path == null || path.isEmpty()) {
            path = request.getParameter("path");
        }
        
        if (path == null || path.isEmpty()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // 移除末尾斜杠
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // 1. 检查是否为固定路由
        if (VALID_ROUTES.contains(path)) {
            return ResponseEntity.ok().build();
        }

        // 2. 检查是否为静态资源
        String lowerPath = path.toLowerCase();
        for (String ext : STATIC_EXTENSIONS) {
            if (lowerPath.endsWith(ext)) {
                return ResponseEntity.ok().build();
            }
        }

        // 3. 检查是否为分页路由 /notes/page/数字
        if (path.matches("/notes/page/\\d+")) {
            return ResponseEntity.ok().build();
        }

        // 4. 检查是否为分类路由 /category/名称/页码
        if (path.matches("/category/[^/]+(/\\d+)?")) {
            return ResponseEntity.ok().build();
        }

        // 5. 检查是否为标签路由 /tag/名称/页码
        if (path.matches("/tag/[^/]+(/\\d+)?")) {
            return ResponseEntity.ok().build();
        }

        // 6. 检查是否为搜索路由 /search/关键词/页码
        if (path.matches("/search/[^/]+(/\\d+)?")) {
            return ResponseEntity.ok().build();
        }

        // 7. 检查是否为博客详情路由 /notes/数字
        if (path.matches("/notes/\\d+")) {
            return ResponseEntity.ok().build();
        }

        // 8. 检查是否为旧版博客路由 /blog/数字 或 /article/数字
        if (path.matches("/blog/\\d+") || path.matches("/article/\\d+")) {
            return ResponseEntity.ok().build();
        }

        // 9. 检查是否为自定义路径（查询数据库）
        String subUrl = path.startsWith("/") ? path.substring(1) : path;
        Object blogResult = blogService.getBlogDetailBySubUrl(subUrl);
        if (blogResult != null) {
            return ResponseEntity.ok().build();
        }

        // 10. 检查是否为 SEO 文件
        if (path.equals("/sitemap.xml") || path.equals("/robots.txt") || path.equals("/llms.txt")) {
            return ResponseEntity.ok().build();
        }

        // 其他情况返回 403（Nginx 会转换为 404）
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
}
