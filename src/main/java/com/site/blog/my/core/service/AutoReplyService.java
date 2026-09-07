package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.AutoReply;

import java.util.List;

public interface AutoReplyService {

    /** 管理端全量列表 */
    List<AutoReply> getAutoReplyList();

    /** 新增键值对，keyword 重复返回 false */
    boolean saveAutoReply(String keyword, String reply);

    /** 修改键值对（keyword 变更需查重），status 传 null 则保持原值 */
    boolean updateAutoReply(Long id, String keyword, String reply, Integer status);

    /** 批量删除 */
    boolean deleteBatch(Integer[] ids);

    /**
     * 微信文本消息匹配固定回复：先整句精确命中，再逐条包含命中（先配置优先）。
     * 未命中返回 null，调用方走 AI 回复。
     */
    String matchReply(String content);
}
