package com.site.blog.my.core.controller.blog;

import cn.hutool.captcha.AbstractCaptcha;
import com.site.blog.my.core.controller.vo.BlogDetailVO;
import com.site.blog.my.core.entity.BlogComment;
import com.site.blog.my.core.entity.BlogLink;
import com.site.blog.my.core.entity.GuestbookMessage;
import com.site.blog.my.core.entity.PageStatEvent;
import com.site.blog.my.core.service.*;
import com.site.blog.my.core.service.impl.PageStatLogServiceImpl;
import com.site.blog.my.core.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
    private PageStatLogService pageStatLogService;

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
        putListFilters(data, null, null);
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 列表页公共筛选数据：类目 chips（带已发布篇数）+ 当前类目 + 当前标签 + 该类目下的标签
     * <p>
     * 三个约束：
     * 1. 技术笔记与生活杂记互不出圈——技术侧不能出现「读书/美食」标签；
     * 2. 标签按「类目 → 标签」二级联动，未选类目时不下发标签，避免一次铺满全部标签；
     * 3. 标签是类目内的收窄条件，故标签列表始终按类目取全量（不随已选标签收窄），
     * 否则选中一个标签后其余标签会全部消失，无法切换。
     *
     * @param categoryName 当前类目；为空表示「全部」，此时只给类目 chips
     * @param tagName      当前标签；可为空
     */
    private void putListFilters(Map<String, Object> data, String categoryName, String tagName) {
        boolean life = categoryName != null && CategoryScope.LIFE_CATEGORY_NAMES.contains(categoryName);
        // 前端据此高亮类目、并决定是否渲染标签行
        data.put("activeCategory", categoryName);
        data.put("activeTag", tagName);
        // 生活类页面只出该类目的标签，不给技术类目 chips
        data.put("categoryFilters", life ? null
                : blogService.getCategoryCountsForScope(CategoryScope.LIFE_CATEGORY_NAMES));
        data.put("hotTags", categoryName == null ? Collections.emptyList()
                : tagService.getBlogTagCountForScope(categoryName, CategoryScope.LIFE_CATEGORY_NAMES));
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
        data.put("hotTags", tagService.getBlogTagCountForScope(null, CategoryScope.LIFE_CATEGORY_NAMES));
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 分类下的文章列表
     *
     * @param tagName 可选：类目内再按标签收窄（/category/Agent/1?tag=xxx）
     */
    @GetMapping("/category/{categoryName}/{page}")
    public Result categoryBlogs(@PathVariable("categoryName") String categoryName,
                                @PathVariable("page") int page,
                                @RequestParam(value = "tag", required = false) String tagName) {
        PageResult blogPageResult = blogService.getBlogsPageByCategoryAndTag(categoryName, tagName, page);
        Map<String, Object> data = new HashMap<>();
        data.put("blogPage", blogPageResult);
        data.put("newBlogs", blogService.getBlogListForIndexPage(1));
        data.put("hotBlogs", blogService.getBlogListForIndexPage(0));
        putListFilters(data, categoryName, tagName);
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
        // 标签页反查归属类目，好让筛选条高亮对应类目并给出同类目的兄弟标签
        putListFilters(data, tagService.getTopCategoryNameByTag(tagName), tagName);
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
        putListFilters(data, null, null);
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
        AbstractCaptcha captcha = (AbstractCaptcha) session.getAttribute("verifyCode");
        if (captcha == null || !captcha.verify(verifyCode)) {
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
     * 获取客户端真实 IP。
     *
     * <p>原实现取 X-Forwarded-For <b>第一段</b>，而第一段是客户端可以随便伪造的，
     * 用它做限流等于没限流（改个 header 就换一个身份）。
     * 统一改走 {@link ClientIpResolver}：CDN 写入的地址在链尾，从后往前取第一个公网地址。
     */
    private String getClientIp(HttpServletRequest request) {
        return ClientIpResolver.resolve(request).getIp();
    }

    /**
     * 页面访问统计埋点。
     *
     * <p><b>这个接口的唯一职责是「用最快速度收下数据」</b>：
     * 取字段 → 塞进有界内存队列 → 立刻返回，<b>不做任何磁盘 IO、DB IO 或锁等待</b>。
     * 真正的落盘（JSONL）由 {@code PageStatLogService} 的单消费者线程批量完成。
     *
     * <p>为什么这样设计：埋点是「高频、低价值、可丢失」的请求。
     * 若在 Tomcat 线程里直接写文件，磁盘抖动或日志盘写满就会让请求线程全部堆积，
     * 反过来把整站接口拖慢——埋点把主站拖挂是最不划算的事故。
     * 队列满时直接丢弃并计数（报表「数据健康」里可见），绝不阻塞请求。
     */
    @PostMapping("/stat")
    public Result stat(HttpServletRequest request, @RequestBody(required = false) Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            return ResultGenerator.genSuccessResult();
        }
        PageStatEvent event = new PageStatEvent();
        // 兼容旧版前端只发 {pagePath}
        event.setPath(text(body, "pagePath", 200));
        if (event.getPath() == null) {
            event.setPath(text(body, "path", 200));
        }
        event.setType(text(body, "type", 16));
        event.setTitle(text(body, "title", 120));
        event.setRef(text(body, "ref", 300));
        event.setVid(text(body, "vid", 64));
        event.setSid(text(body, "sid", 64));
        event.setScr(text(body, "scr", 20));
        event.setLang(text(body, "lang", 20));
        event.setTs(text(body, "ts", 32));
        event.setDur(number(body.get("dur")));
        event.setEntry(Boolean.TRUE.equals(body.get("entry")));

        event.setSt(LocalDateTime.now().format(STAT_TIME));
        ClientIpResolver.Resolved ip = ClientIpResolver.resolve(request);
        event.setIp(ip.getIp());
        event.setIpSrc(ip.getSource());
        event.setXff(ip.getRawXff());
        event.setInternal(ip.isInternal());
        event.setUa(clip(request.getHeader("User-Agent"), 400));

        // 非法路径在入队前就丢掉，避免脏数据占用队列容量
        if (PageStatLogServiceImpl.normalizePath(event.getPath()) == null) {
            return ResultGenerator.genSuccessResult();
        }
        pageStatLogService.offer(event);
        return ResultGenerator.genSuccessResult();
    }

    /** 固定宽度的时间格式，避免每次上报都构造 DateTimeFormatter */
    private static final DateTimeFormatter STAT_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String text(Map<String, Object> body, String key, int max) {
        Object o = body.get(key);
        if (o == null) {
            return null;
        }
        String s = o instanceof String ? (String) o : String.valueOf(o);
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static Long number(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        if (o instanceof String) {
            try {
                return Long.parseLong(((String) o).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
