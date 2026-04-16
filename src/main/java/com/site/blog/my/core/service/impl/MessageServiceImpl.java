package com.site.blog.my.core.service.impl;

import com.site.blog.my.core.dao.MessageMapper;
import com.site.blog.my.core.entity.Message;
import com.site.blog.my.core.service.ChatService;
import com.site.blog.my.core.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ChatService chatService;

    @Override
    public Message handleMessage(Message message) {
        Message replyMessage = new Message();

        //查询msgId为当前的消息的Id防止重试
        List<Message> messagesTemp = messageMapper.selectByMsgId(message.getMsgId());
        if (messagesTemp != null && !messagesTemp.isEmpty()){
            if (messagesTemp.size() == 1){
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            messagesTemp = messageMapper.selectByMsgId(message.getMsgId());
            for (Message msg : messagesTemp){
                if (msg.getToUser().equals(message.getFromUser())){
                    return msg;
                }
            }
            throw new RuntimeException("Time out!");
        }else {
            // 查询历史消息
            List<Message> historyMessages = messageMapper.selectByUser(message.getFromUser());
            // 插入新消息
            messageMapper.insert(message);
            if (message.getMsgType().equals("text")){
                replyMessage.setContent(chatService.chat(message.getContent(), message.getFromUser(), historyMessages));
            }else if (message.getMsgType().equals("image")){
                replyMessage.setContent(chatService.vision(message.getContent()));
            }

            replyMessage.setToUser(message.getFromUser());
            replyMessage.setFromUser(message.getToUser());
            replyMessage.setMsgType("text");
            replyMessage.setCreateTime(new java.util.Date());
            replyMessage.setMsgId(message.getMsgId());

            // 插入回复消息
            messageMapper.insert(replyMessage);

            return replyMessage;
        }
    }
}
