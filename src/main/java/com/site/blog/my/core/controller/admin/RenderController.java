package com.site.blog.my.core.controller.admin;

import com.site.blog.my.core.service.StaticRenderService;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 静态渲染管理接口
 */
@RestController
@RequestMapping("/admin/render")
public class RenderController {

    private static final Logger logger = LoggerFactory.getLogger(RenderController.class);

    @Autowired
    private StaticRenderService staticRenderService;

    /**
     * 渲染所有已发布博客
     */
    @PostMapping("/all")
    public Result renderAll() {
        logger.info("手动触发全量渲染");
        StaticRenderService.RenderResult result = staticRenderService.renderAllBlogs();
        
        Map<String, Object> data = new HashMap<>();
        data.put("success", result.isSuccess());
        data.put("message", result.getMessage());
        if (result.getOutput() != null) {
            data.put("output", result.getOutput());
        }
        
        if (result.isSuccess()) {
            return ResultGenerator.genSuccessResult(data);
        } else {
            return ResultGenerator.genFailResult(result.getMessage());
        }
    }

    /**
     * 渲染单个博客
     */
    @PostMapping("/blog/{blogId}")
    public Result renderBlog(@PathVariable Long blogId) {
        logger.info("手动触发渲染博客: {}", blogId);
        StaticRenderService.RenderResult result = staticRenderService.renderBlog(blogId);
        
        if (result.isSuccess()) {
            return ResultGenerator.genSuccessResult(result.getMessage());
        } else {
            return ResultGenerator.genFailResult(result.getMessage());
        }
    }

    /**
     * 获取待渲染的 URL 列表（供脚本使用）
     */
    @GetMapping("/urls")
    public Result getUrls() {
        List<String> urls = staticRenderService.getAllBlogUrls();
        return ResultGenerator.genSuccessResult(urls);
    }

    /**
     * 获取渲染状态
     */
    @GetMapping("/status")
    public Result getStatus() {
        Map<String, Object> data = new HashMap<>();
        data.put("rendering", staticRenderService.isRendering());
        return ResultGenerator.genSuccessResult(data);
    }
}
