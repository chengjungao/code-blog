package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.GuestbookMessageMapper;
import com.site.blog.my.core.entity.GuestbookMessage;
import com.site.blog.my.core.service.GuestbookMessageService;
import com.site.blog.my.core.util.PageQueryUtil;
import com.site.blog.my.core.util.PageResult;
import com.site.blog.my.core.util.SensitiveWordFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GuestbookMessageServiceImpl implements GuestbookMessageService {

    @Autowired
    private GuestbookMessageMapper guestbookMessageMapper;

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @Override
    public String addMessage(GuestbookMessage message) {
        // 敏感词检测
        Set<String> badWords = sensitiveWordFilter.getSensitiveWords(message.getMessageBody());
        if (!badWords.isEmpty()) {
            // 过滤掉敏感词后保存，状态为待审核
            String filtered = sensitiveWordFilter.filter(message.getMessageBody());
            message.setMessageBody(filtered);
        }
        // 昵称也做敏感词检测
        Set<String> nameBadWords = sensitiveWordFilter.getSensitiveWords(message.getNickname());
        if (!nameBadWords.isEmpty()) {
            message.setNickname(sensitiveWordFilter.filter(message.getNickname()));
        }
        message.setMessageStatus((byte) 0); // 待审核
        message.setIsDeleted((byte) 0);
        message.setMessageCreateTime(new Date());
        return guestbookMessageMapper.insertSelective(message) > 0 ? null : "保存失败";
    }

    @Override
    public PageResult getMessagesPage(PageQueryUtil pageUtil) {
        List<GuestbookMessage> messages = guestbookMessageMapper.findMessageList(pageUtil);
        int total = guestbookMessageMapper.getTotalMessages(pageUtil);
        return new PageResult(messages, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    public int getTotalApprovedMessages() {
        return guestbookMessageMapper.getTotalApprovedMessages();
    }

    @Override
    public Boolean checkDone(Integer[] ids) {
        return guestbookMessageMapper.checkDone(ids) > 0;
    }

    @Override
    public Boolean deleteBatch(Integer[] ids) {
        return guestbookMessageMapper.deleteBatch(ids) > 0;
    }

    @Override
    public Boolean reply(Long messageId, String replyBody) {
        GuestbookMessage message = guestbookMessageMapper.selectByPrimaryKey(messageId);
        if (message == null || message.getMessageStatus() != 1) {
            return false;
        }
        message.setReplyBody(replyBody);
        message.setReplyCreateTime(new Date());
        return guestbookMessageMapper.updateByPrimaryKeySelective(message) > 0;
    }

    @Override
    public PageResult getMessagePageByPageNum(int page) {
        Map params = new HashMap();
        params.put("page", page);
        params.put("limit", 10);
        params.put("messageStatus", 1); // 只查审核通过的
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        List<GuestbookMessage> messages = guestbookMessageMapper.findMessageList(pageUtil);
        int total = guestbookMessageMapper.getTotalMessages(pageUtil);
        return new PageResult(messages, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    public Set<String> getSensitiveWords(String text) {
        return sensitiveWordFilter.getSensitiveWords(text);
    }
}
