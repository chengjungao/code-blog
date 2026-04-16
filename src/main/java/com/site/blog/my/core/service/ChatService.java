package com.site.blog.my.core.service;

import com.site.blog.my.core.entity.Message;

import java.util.List;

public interface ChatService {
    public String chat(String content, String user,List<Message> history);

    public String vision(String content);
}
