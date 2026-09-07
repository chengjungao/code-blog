package com.site.blog.my.core.controller.admin;

import com.site.blog.my.core.service.AutoReplyService;
import com.site.blog.my.core.util.Result;
import com.site.blog.my.core.util.ResultGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 微信自动回复键值对管理 Controller
 * 仅保留 JSON API 接口，供 Vue 管理后台"系统配置"页调用
 */
@Controller
@RequestMapping("/admin")
public class AutoReplyController {

    @Resource
    private AutoReplyService autoReplyService;

    /**
     * 自动回复规则列表
     */
    @RequestMapping(value = "/autoReplies/list", method = RequestMethod.GET)
    @ResponseBody
    public Result list() {
        return ResultGenerator.genSuccessResult(autoReplyService.getAutoReplyList());
    }

    /**
     * 新增规则
     */
    @RequestMapping(value = "/autoReplies/save", method = RequestMethod.POST)
    @ResponseBody
    public Result save(@RequestParam("keyword") String keyword,
                       @RequestParam("reply") String reply) {
        if (StringUtils.isEmpty(keyword) || StringUtils.isEmpty(reply)) {
            return ResultGenerator.genFailResult("关键词和回复内容不能为空！");
        }
        if (autoReplyService.saveAutoReply(keyword.trim(), reply.trim())) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("保存失败，关键词可能已存在");
        }
    }

    /**
     * 修改规则（status 用于启停开关）
     */
    @RequestMapping(value = "/autoReplies/update", method = RequestMethod.POST)
    @ResponseBody
    public Result update(@RequestParam("id") Long id,
                         @RequestParam("keyword") String keyword,
                         @RequestParam("reply") String reply,
                         @RequestParam(value = "status", required = false) Integer status) {
        if (StringUtils.isEmpty(keyword) || StringUtils.isEmpty(reply)) {
            return ResultGenerator.genFailResult("关键词和回复内容不能为空！");
        }
        if (autoReplyService.updateAutoReply(id, keyword.trim(), reply.trim(), status)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("修改失败，关键词可能已存在");
        }
    }

    /**
     * 批量删除
     */
    @RequestMapping(value = "/autoReplies/delete", method = RequestMethod.POST)
    @ResponseBody
    public Result delete(@RequestBody Integer[] ids) {
        if (ids.length < 1) {
            return ResultGenerator.genFailResult("参数异常！");
        }
        if (autoReplyService.deleteBatch(ids)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("删除失败");
        }
    }
}
