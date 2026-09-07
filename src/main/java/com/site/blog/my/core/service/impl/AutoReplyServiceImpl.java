package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.AutoReplyMapper;
import com.site.blog.my.core.entity.AutoReply;
import com.site.blog.my.core.service.AutoReplyService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;

@Service
public class AutoReplyServiceImpl implements AutoReplyService {

    @Resource
    private AutoReplyMapper autoReplyMapper;

    @Override
    public List<AutoReply> getAutoReplyList() {
        return autoReplyMapper.selectAllList();
    }

    @Override
    public boolean saveAutoReply(String keyword, String reply) {
        if (StringUtils.isEmpty(keyword) || StringUtils.isEmpty(reply)) {
            return false;
        }
        if (autoReplyMapper.selectByKeyword(keyword) != null) {
            return false;
        }
        AutoReply record = new AutoReply();
        record.setKeyword(keyword);
        record.setReply(reply);
        record.setStatus(1);
        return autoReplyMapper.insertSelective(record) > 0;
    }

    @Override
    public boolean updateAutoReply(Long id, String keyword, String reply, Integer status) {
        if (id == null) {
            return false;
        }
        AutoReply old = autoReplyMapper.selectByPrimaryKey(id);
        if (old == null) {
            return false;
        }
        // 关键词变更时查重（排除自身）
        if (!StringUtils.isEmpty(keyword) && !keyword.equals(old.getKeyword())) {
            AutoReply dup = autoReplyMapper.selectByKeyword(keyword);
            if (dup != null && !dup.getId().equals(id)) {
                return false;
            }
        }
        AutoReply record = new AutoReply();
        record.setId(id);
        if (!StringUtils.isEmpty(keyword)) {
            record.setKeyword(keyword);
        }
        if (!StringUtils.isEmpty(reply)) {
            record.setReply(reply);
        }
        if (status != null) {
            record.setStatus(status);
        }
        return autoReplyMapper.updateByPrimaryKeySelective(record) > 0;
    }

    @Override
    public boolean deleteBatch(Integer[] ids) {
        if (ids == null || ids.length < 1) {
            return false;
        }
        return autoReplyMapper.deleteBatch(ids) > 0;
    }

    @Override
    public String matchReply(String content) {
        if (StringUtils.isEmpty(content)) {
            return null;
        }
        String text = content.trim();
        String textLower = text.toLowerCase(Locale.ROOT);
        // 规则量小，一次加载启用列表本地匹配；比较统一转小写（忽略大小写）
        List<AutoReply> enabledList = autoReplyMapper.selectEnabledList();
        // 1. 整句精确命中（忽略大小写）
        for (AutoReply rule : enabledList) {
            String keyword = rule.getKeyword();
            if (keyword != null && textLower.equals(keyword.toLowerCase(Locale.ROOT))) {
                return rule.getReply();
            }
        }
        // 2. 包含命中（忽略大小写，先配置优先）
        for (AutoReply rule : enabledList) {
            String keyword = rule.getKeyword();
            if (keyword != null && textLower.contains(keyword.toLowerCase(Locale.ROOT))) {
                return rule.getReply();
            }
        }
        return null;
    }
}
