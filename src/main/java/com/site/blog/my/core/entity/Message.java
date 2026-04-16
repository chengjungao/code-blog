package com.site.blog.my.core.entity;


import java.util.Date;

public class Message {
    private Long id;

    private String fromUser;
    private String toUser;
    private String content;
    private String msgType;
    private Date createTime;

    private String msgId;

    public Message( ){
        super();
    }

    public Message(String fromUser, String toUser, String content, String msgType, Date createTime, String msgId) {
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
        this.msgType = msgType;
        this.createTime = createTime;
        this.msgId = msgId;
    }

    // Getters 和 Setters
    public Long getId() { return id; }
    public String getFromUser() { return fromUser; }
    public String getToUser() { return toUser; }
    public String getContent() { return content; }
    public String getMsgType() { return msgType; }
    public Date getCreateTime() { return createTime; }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFromUser(String fromUser) {
        this.fromUser = fromUser;
    }

    public void setToUser(String toUser) {
        this.toUser = toUser;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getMsgId() {
        return msgId;
    }

    public void setMsgId(String msgId) {
        this.msgId = msgId;
    }
}
