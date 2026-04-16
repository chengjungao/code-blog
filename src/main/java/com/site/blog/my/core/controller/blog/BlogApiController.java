package com.site.blog.my.core.controller.blog;

import cn.hutool.captcha.ShearCaptcha;
import com.site.blog.my.core.controller.vo.BlogDetailVO;
import com.site.blog.my.core.entity.BlogComment;
import com.site.blog.my.core.entity.BlogLink;
import com.site.blog.my.core.service.*;
import com.site.blog.my.core.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 博客前台 JSON API
 * 所有接口前缀 /blog/api，供 Vue 前台调用
 */
@RestController
@RequestMapping("/blog/api")
public class BlogApiController {

    @Resource
    BlogService blogService;
    @Resource
    private TagService tagService;
    @Resource
    private LinkService linkService;
    @Resource
    private CommentService commentService;
    @Resource
    private ConfigService configService;
    @Resource
    private CategoryService categoryService;

    /**
     * 获取网站配置
     */
    @GetMapping("/config")
    public Result getConfig() {
        return ResultGenerator.genSuccessResult(configService.getAllConfigs());
    }

    /**
     * 首页分页数据（含侧边栏）
     */
    @GetMapping("/index/{page}")
    public Result index(@PathVariable("page") int page) {
        PageResult blogPageResult = blogService.getBlogsForIndexPage(page);
        Map<String, Object> data = new HashMap<>();
        data.put("blogPage", blogPageResult);
        data.put("newBlogs", blogService.getBlogListForIndexPage(1));
        data.put("hotBlogs", blogService.getBlogListForIndexPage(0));
        data.put("hotTags", tagService.getBlogTagCountForIndex());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 文章详情
     */
    @GetMapping("/blog/{blogId}")
    public Result detail(@PathVariable("blogId") Long blogId,
                         @RequestParam(value = "commentPage", required = false, defaultValue = "1") Integer commentPage) {
        BlogDetailVO blogDetailVO = blogService.getBlogDetail(blogId);
        Map<String, Object> data = new HashMap<>();
        data.put("blog", blogDetailVO);
        if (blogDetailVO != null) {
            data.put("comments", commentService.getCommentPageByBlogIdAndPageNum(blogId, commentPage));
        }
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 根据 subUrl 获取文章（关于页等）
     */
    @GetMapping("/page/{subUrl}")
    public Result pageBySubUrl(@PathVariable("subUrl") String subUrl) {
        BlogDetailVO blogDetailVO = blogService.getBlogDetailBySubUrl(subUrl);
        return ResultGenerator.genSuccessResult(blogDetailVO);
    }

    /**
     * 分类列表
     */
    @GetMapping("/categories")
    public Result categories() {
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryService.getAllCategories());
        data.put("hotTags", tagService.getBlogTagCountForIndex());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 分类下的文章列表
     */
    @GetMapping("/category/{categoryName}/{page}")
    public Result categoryBlogs(@PathVariable("categoryName") String categoryName,
                                @PathVariable("page") int page) {
        PageResult blogPageResult = blogService.getBlogsPageByCategory(categoryName, page);
        Map<String, Object> data = new HashMap<>();
        data.put("blogPage", blogPageResult);
        data.put("newBlogs", blogService.getBlogListForIndexPage(1));
        data.put("hotBlogs", blogService.getBlogListForIndexPage(0));
        data.put("hotTags", tagService.getBlogTagCountForIndex());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 标签下的文章列表
     */
    @GetMapping("/tag/{tagName}/{page}")
    public Result tagBlogs(@PathVariable("tagName") String tagName,
                           @PathVariable("page") int page) {
        PageResult blogPageResult = blogService.getBlogsPageByTag(tagName, page);
        Map<String, Object> data = new HashMap<>();
        data.put("blogPage", blogPageResult);
        data.put("newBlogs", blogService.getBlogListForIndexPage(1));
        data.put("hotBlogs", blogService.getBlogListForIndexPage(0));
        data.put("hotTags", tagService.getBlogTagCountForIndex());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 搜索文章
     */
    @GetMapping("/search/{keyword}/{page}")
    public Result searchBlogs(@PathVariable("keyword") String keyword,
                              @PathVariable("page") int page) {
        PageResult blogPageResult = blogService.getBlogsPageBySearch(keyword, page);
        Map<String, Object> data = new HashMap<>();
        data.put("blogPage", blogPageResult);
        data.put("newBlogs", blogService.getBlogListForIndexPage(1));
        data.put("hotBlogs", blogService.getBlogListForIndexPage(0));
        data.put("hotTags", tagService.getBlogTagCountForIndex());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 友情链接
     */
    @GetMapping("/links")
    public Result links() {
        Map<Byte, List<BlogLink>> linkMap = linkService.getLinksForLinkPage();
        return ResultGenerator.genSuccessResult(linkMap);
    }

    /**
     * 提交评论
     */
    @PostMapping("/comment")
    public Result comment(HttpServletRequest request, HttpSession session,
                          @RequestParam Long blogId, @RequestParam String verifyCode,
                          @RequestParam String commentator, @RequestParam String email,
                          @RequestParam(required = false) String websiteUrl,
                          @RequestParam String commentBody) {
        if (StringUtils.isEmpty(verifyCode)) {
            return ResultGenerator.genFailResult("验证码不能为空");
        }
        ShearCaptcha shearCaptcha = (ShearCaptcha) session.getAttribute("verifyCode");
        if (shearCaptcha == null || !shearCaptcha.verify(verifyCode)) {
            return ResultGenerator.genFailResult("验证码错误");
        }
        String ref = request.getHeader("Referer");
        if (StringUtils.isEmpty(ref)) {
            return ResultGenerator.genFailResult("非法请求");
        }
        if (null == blogId || blogId < 0) {
            return ResultGenerator.genFailResult("非法请求");
        }
        if (StringUtils.isEmpty(commentator)) {
            return ResultGenerator.genFailResult("请输入称呼");
        }
        if (StringUtils.isEmpty(email)) {
            return ResultGenerator.genFailResult("请输入邮箱地址");
        }
        if (!PatternUtil.isEmail(email)) {
            return ResultGenerator.genFailResult("请输入正确的邮箱地址");
        }
        if (StringUtils.isEmpty(commentBody)) {
            return ResultGenerator.genFailResult("请输入评论内容");
        }
        if (commentBody.trim().length() > 200) {
            return ResultGenerator.genFailResult("评论内容过长");
        }
        BlogComment comment = new BlogComment();
        comment.setBlogId(blogId);
        comment.setCommentator(MyBlogUtils.cleanString(commentator));
        comment.setEmail(email);
        if (PatternUtil.isURL(websiteUrl)) {
            comment.setWebsiteUrl(websiteUrl);
        }
        comment.setCommentBody(MyBlogUtils.cleanString(commentBody));
        return ResultGenerator.genSuccessResult(commentService.addComment(comment));
    }
}
