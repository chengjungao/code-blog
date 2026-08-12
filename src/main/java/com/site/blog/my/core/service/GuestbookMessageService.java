package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.GuestbookMessage;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;

import java.util.Set;

public interface GuestbookMessageService {

    /**
     * 提交留言（含敏感词检测）
     * @return null=成功, 否则返回拒绝原因
     */
    String addMessage(GuestbookMessage message);

    /**
     * 后台分页查询留言
     */
    PageResult getMessagesPage(PageQueryUtil pageUtil);

    /**
     * 已审核通过留言总数
     */
    int getTotalApprovedMessages();

    /**
     * 批量审核通过
     */
    Boolean checkDone(Integer[] ids);

    /**
     * 批量删除（软删除）
     */
    Boolean deleteBatch(Integer[] ids);

    /**
     * 回复留言
     */
    Boolean reply(Long messageId, String replyBody);

    /**
     * 前台分页查询已审核留言
     */
    PageResult getMessagePageByPageNum(int page);

    /**
     * 检查文本中的敏感词
     */
    Set<String> getSensitiveWords(String text);
}
