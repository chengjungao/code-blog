package com.site.blog.my.core.controller.common;
import com.site.blog.my.core.entity.Message;
import com.site.blog.my.core.service.MessageService;
import com.site.blog.my.core.util.SignUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @Value("${token:}")
    private String token;

    @GetMapping("/wechat")
    public String verify(
            @RequestParam("signature") String signature,
            @RequestParam("timestamp") String timestamp,
            @RequestParam("nonce") String nonce,
            @RequestParam("echostr") String echostr) {

        if (SignUtil.checkSignature(signature, timestamp, nonce,token)) {
            return echostr;
        }
        return "校验失败";
    }

    @PostMapping("/wechat")
    public String handleMessage(HttpServletRequest request) {
        try {
            BufferedReader reader = request.getReader();
            StringBuilder xmlData = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                xmlData.append(line);
            }

            String fromUser = extractTagValue(xmlData.toString(), "FromUserName");
            String toUser = extractTagValue(xmlData.toString(), "ToUserName");
            String msgType = extractTagValue(xmlData.toString(), "MsgType");
            String msgId = extractTagIntValue(xmlData.toString(), "MsgId");
            String content = null;
            if (msgType.equals("event")){
                return buildReplyMessage(new Message(toUser,fromUser,"谢谢你长的如此好看，还关注我！","text",new Date(),msgId));
            }else if (msgType.equals("text")){
                content= extractTagValue(xmlData.toString(), "Content");
            }else if (msgType.equals("image")){
                content= extractTagValue(xmlData.toString(), "PicUrl");
            }

            Message message = new Message(fromUser, toUser, content, msgType, new Date(),msgId);
            return buildReplyMessage(messageService.handleMessage(message));

        } catch (IOException e) {
            throw new RuntimeException("Time Out! Please retry!");
        }
    }

    // 解析XML节点值
    // 提取 XML 标签值的方法
    private String extractTagValue(String xml, String tagName) {
        String startTag = "<" + tagName + "><![CDATA[";
        String endTag = "]]></" + tagName + ">";

        return extractTagValue(startTag, endTag, xml);
    }

    private String extractTagIntValue(String xml, String tagName) {
        String startTag = "<" + tagName + ">";
        String endTag = "</" + tagName + ">";

       return extractTagValue(startTag, endTag, xml);
    }

    private String extractTagValue(String startTag, String endTag,String xml) {
        int startIndex = xml.indexOf(startTag);
        int endIndex = xml.indexOf(endTag);

        if (startIndex != -1 && endIndex != -1) {
            return xml.substring(startIndex + startTag.length(), endIndex).trim();
        }
        return null;

    }

    // 构建回复XML消息
    private String buildReplyMessage(Message reply) {
        return "<xml>" +
                "<ToUserName><![CDATA[" + reply.getToUser() + "]]></ToUserName>" +
                "<FromUserName><![CDATA[" + reply.getFromUser() + "]]></FromUserName>" +
                "<CreateTime>" + System.currentTimeMillis() / 1000 + "</CreateTime>" +
                "<MsgType><![CDATA[text]]></MsgType>" +
                "<Content><![CDATA[" + reply.getContent() + "]]></Content>" +
                "</xml>";
    }
}

