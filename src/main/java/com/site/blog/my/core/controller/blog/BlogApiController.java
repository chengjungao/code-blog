package com.site.blog.my.core.controller.blog;

import cn.hutool.captcha.ShearCaptcha;
import com.site.blog.my.core.controller.vo.BlogDetailVO;
import com.site.blog.my.core.entity.BlogComment;
import com.site.blog.my.core.entity.BlogLink;
import com.site.blog.my.core.entity.GuestbookMessage;
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
    @Resource
    private GuestbookMessageService guestbookMessageService;
    @Resource
    private ChatService chatService;
    @Resource
    private IpRateLimiter ipRateLimiter;
    @Resource
    private VisitStatService visitStatService;

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
    public Result comment(HttpServletRequest request, HttpSession session, @RequestBody Map<String, Object> body) {
        String verifyCode = body.get("verifyCode") != null ? body.get("verifyCode").toString() : null;
        Long blogId = body.get("blogId") != null ? Long.valueOf(body.get("blogId").toString()) : null;
        String commentator = body.get("commentator") != null ? body.get("commentator").toString() : null;
        String email = body.get("email") != null ? body.get("email").toString() : null;
        String websiteUrl = body.get("websiteUrl") != null ? body.get("websiteUrl").toString() : null;
        String commentBody = body.get("commentBody") != null ? body.get("commentBody").toString() : null;
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

    /**
     * 留言板 - 分页获取已审核留言
     */
    @GetMapping("/messages/{page}")
    public Result messages(@PathVariable("page") int page) {
        PageResult messagePageResult = guestbookMessageService.getMessagePageByPageNum(page);
        Map<String, Object> data = new HashMap<>();
        data.put("messagePage", messagePageResult);
        data.put("totalMessages", guestbookMessageService.getTotalApprovedMessages());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 留言板 - 提交留言（需审核 + 敏感词自动过滤）
     */
    @PostMapping("/message")
    public Result message(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String nickname = body.get("nickname") != null ? body.get("nickname").toString() : null;
        String email = body.get("email") != null ? body.get("email").toString() : null;
        String avatar = body.get("avatar") != null ? body.get("avatar").toString() : null;
        String messageBody = body.get("messageBody") != null ? body.get("messageBody").toString() : null;
        if (StringUtils.isEmpty(nickname)) {
            return ResultGenerator.genFailResult("请输入昵称");
        }
        if (nickname.trim().length() > 30) {
            return ResultGenerator.genFailResult("昵称过长");
        }
        if (StringUtils.isEmpty(email)) {
            return ResultGenerator.genFailResult("请输入邮箱地址");
        }
        if (!PatternUtil.isEmail(email)) {
            return ResultGenerator.genFailResult("请输入正确的邮箱地址");
        }
        if (StringUtils.isEmpty(messageBody)) {
            return ResultGenerator.genFailResult("请输入留言内容");
        }
        if (messageBody.trim().length() > 500) {
            return ResultGenerator.genFailResult("留言内容过长");
        }
        GuestbookMessage message = new GuestbookMessage();
        message.setNickname(MyBlogUtils.cleanString(nickname));
        message.setEmail(email);
        if (avatar != null && !avatar.trim().isEmpty()) {
            message.setAvatar(avatar.trim());
        }
        message.setMessageBody(MyBlogUtils.cleanString(messageBody));
        message.setMessageIp(request.getRemoteAddr());
        String error = guestbookMessageService.addMessage(message);
        if (error != null) {
            return ResultGenerator.genFailResult(error);
        }
        return ResultGenerator.genSuccessResult("留言提交成功，等待审核");
    }

    /**
     * 智能分身 - 对话
     */
    @PostMapping("/assistant")
    public Result assistant(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        // 防爬：基于 IP 限流
        String clientIp = getClientIp(request);
        if (!ipRateLimiter.tryAcquire(clientIp)) {
            return ResultGenerator.genFailResult("提问太频繁啦，请稍后再试～");
        }
        String message = (String) body.get("message");
        String history = body.get("history") != null ? body.get("history").toString() : null;
        if (StringUtils.isEmpty(message)) {
            return ResultGenerator.genFailResult("消息不能为空");
        }
        if (message.trim().length() > 500) {
            return ResultGenerator.genFailResult("消息过长，请精简后重试");
        }
        String reply = chatService.assistantChat(message.trim(), history);
        // 强转 Object，避免命中 genSuccessResult(String) 重载把内容塞进 message 字段
        return ResultGenerator.genSuccessResult((Object) reply);
    }

    /**
     * 获取客户端真实 IP（兼容 nginx 反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多级代理时取第一个非 unknown 的 IP
            int idx = ip.indexOf(',');
            if (idx > 0) {
                return ip.substring(0, idx).trim();
            }
            return ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 页面访问统计埋点（实时数据只进内存，定时聚合写 DB）
     */
    @PostMapping("/stat")
    public Result stat(HttpServletRequest request, @RequestBody Map<String, Object> body) {
        String pagePath = body.get("pagePath") != null ? body.get("pagePath").toString() : null;
        if (StringUtils.isEmpty(pagePath)) {
            return ResultGenerator.genSuccessResult();
        }
        if (pagePath.length() > 200) {
            pagePath = pagePath.substring(0, 200);
        }
        visitStatService.record(pagePath, getClientIp(request));
        return ResultGenerator.genSuccessResult();
    }
}
