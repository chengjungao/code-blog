package com.site.blog.my.core.controller.admin;

import com.site.blog.my.core.entity.AdminUser;
import com.site.blog.my.core.service.*;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * REST API Controller — 供 Vue 前端调用
 * 所有接口返回 JSON，路径前缀 /admin/api
 */
@Controller
@RequestMapping("/admin/api")
public class AdminApiController {

    @Resource
    private AdminUserService adminUserService;
    @Resource
    private BlogService blogService;
    @Resource
    private CategoryService categoryService;
    @Resource
    private LinkService linkService;
    @Resource
    private TagService tagService;
    @Resource
    private CommentService commentService;
    @Resource
    private ConfigService configService;

    /**
     * 登录接口
     */
    @PostMapping("/login")
    @ResponseBody
    public Result login(@RequestParam("userName") String userName,
                        @RequestParam("password") String password,
                        @RequestParam("verifyCode") String verifyCode,
                        HttpSession session) {
        if (StringUtils.isEmpty(verifyCode)) {
            return ResultGenerator.genFailResult("验证码不能为空");
        }
        if (StringUtils.isEmpty(userName) || StringUtils.isEmpty(password)) {
            return ResultGenerator.genFailResult("用户名或密码不能为空");
        }
        cn.hutool.captcha.ShearCaptcha shearCaptcha =
                (cn.hutool.captcha.ShearCaptcha) session.getAttribute("verifyCode");
        if (shearCaptcha == null || !shearCaptcha.verify(verifyCode)) {
            return ResultGenerator.genFailResult("验证码错误");
        }
        AdminUser adminUser = adminUserService.login(userName, password);
        if (adminUser != null) {
            session.setAttribute("loginUser", adminUser.getNickName());
            session.setAttribute("loginUserId", adminUser.getAdminUserId());
            Map<String, Object> data = new HashMap<>();
            data.put("token", session.getId());
            data.put("nickName", adminUser.getNickName());
            data.put("loginUserName", adminUser.getLoginUserName());
            return ResultGenerator.genSuccessResult(data);
        } else {
            return ResultGenerator.genFailResult("登陆失败");
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @ResponseBody
    public Result logout(HttpServletRequest request) {
        request.getSession().removeAttribute("loginUserId");
        request.getSession().removeAttribute("loginUser");
        request.getSession().removeAttribute("errorMsg");
        return ResultGenerator.genSuccessResult("退出成功");
    }

    /**
     * Dashboard 统计数据
     */
    @GetMapping("/dashboard")
    @ResponseBody
    public Result dashboard(HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();
        data.put("blogCount", blogService.getTotalBlogs());
        data.put("commentCount", commentService.getTotalComments());
        data.put("categoryCount", categoryService.getTotalCategories());
        data.put("tagCount", tagService.getTotalTags());
        data.put("linkCount", linkService.getTotalLinks());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/profile")
    @ResponseBody
    public Result profile(HttpServletRequest request) {
        Integer loginUserId = (Integer) request.getSession().getAttribute("loginUserId");
        if (loginUserId == null) {
            return ResultGenerator.genFailResult("未登录");
        }
        AdminUser adminUser = adminUserService.getUserDetailById(loginUserId);
        if (adminUser == null) {
            return ResultGenerator.genFailResult("用户不存在");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("loginUserName", adminUser.getLoginUserName());
        data.put("nickName", adminUser.getNickName());
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 修改密码
     */
    @PostMapping("/profile/password")
    @ResponseBody
    public Result passwordUpdate(HttpServletRequest request,
                                 @RequestParam("originalPassword") String originalPassword,
                                 @RequestParam("newPassword") String newPassword) {
        if (StringUtils.isEmpty(originalPassword) || StringUtils.isEmpty(newPassword)) {
            return ResultGenerator.genFailResult("参数不能为空");
        }
        Integer loginUserId = (Integer) request.getSession().getAttribute("loginUserId");
        if (loginUserId == null) {
            return ResultGenerator.genFailResult("未登录");
        }
        if (adminUserService.updatePassword(loginUserId, originalPassword, newPassword)) {
            request.getSession().removeAttribute("loginUserId");
            request.getSession().removeAttribute("loginUser");
            request.getSession().removeAttribute("errorMsg");
            return ResultGenerator.genSuccessResult("修改成功");
        } else {
            return ResultGenerator.genFailResult("修改失败");
        }
    }

    /**
     * 修改昵称
     */
    @PostMapping("/profile/name")
    @ResponseBody
    public Result nameUpdate(HttpServletRequest request,
                             @RequestParam("loginUserName") String loginUserName,
                             @RequestParam("nickName") String nickName) {
        if (StringUtils.isEmpty(loginUserName) || StringUtils.isEmpty(nickName)) {
            return ResultGenerator.genFailResult("参数不能为空");
        }
        Integer loginUserId = (Integer) request.getSession().getAttribute("loginUserId");
        if (loginUserId == null) {
            return ResultGenerator.genFailResult("未登录");
        }
        if (adminUserService.updateName(loginUserId, loginUserName, nickName)) {
            request.getSession().setAttribute("loginUser", nickName);
            return ResultGenerator.genSuccessResult("修改成功");
        } else {
            return ResultGenerator.genFailResult("修改失败");
        }
    }

    /**
     * 获取博客编辑页所需数据（分类列表 + 博客详情）
     */
    @GetMapping("/blogs/edit")
    @ResponseBody
    public Result blogEditData(@RequestParam(value = "blogId", required = false) Long blogId) {
        Map<String, Object> data = new HashMap<>();
        data.put("categories", categoryService.getAllCategories());
        if (blogId != null && blogId > 0) {
            data.put("blog", blogService.getBlogById(blogId));
        }
        return ResultGenerator.genSuccessResult(data);
    }

    /**
     * 获取所有配置项
     */
    @GetMapping("/configurations")
    @ResponseBody
    public Result configurations() {
        return ResultGenerator.genSuccessResult(configService.getAllConfigs());
    }
}
