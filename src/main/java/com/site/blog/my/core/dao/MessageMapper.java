package com.site.blog.my.core.dao;

import com.site.blog.my.core.entity.AdminUser;
import com.site.blog.my.core.entity.Message;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MessageMapper {
    int insert(Message record);

    List<Message> selectByUser(@Param("user") String user);


    List<Message> selectByMsgId(@Param("msgId") String msgId);

}