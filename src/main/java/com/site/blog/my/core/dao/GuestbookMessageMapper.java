package com.site.blog.my.core.dao;

import com.site.blog.my.core.entity.GuestbookMessage;

import java.util.List;
import java.util.Map;

public interface GuestbookMessageMapper {
    int deleteByPrimaryKey(Long messageId);

    int insert(GuestbookMessage record);

    int insertSelective(GuestbookMessage record);

    GuestbookMessage selectByPrimaryKey(Long messageId);

    int updateByPrimaryKeySelective(GuestbookMessage record);

    int updateByPrimaryKey(GuestbookMessage record);

    List<GuestbookMessage> findMessageList(Map map);

    int getTotalMessages(Map map);

    int checkDone(Integer[] ids);

    int deleteBatch(Integer[] ids);

    int getTotalApprovedMessages();
}
