package com.site.blog.my.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.site.blog.my.core.entity.Message;
import com.site.blog.my.core.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${chat.server.url:}")
    private String url;

    @Value("${token:}")
    private String token;

    @Value("${vision.server.url:}")
    private String visionServerUrl;

    @Value("${vision.token:}")
    private String visionToken;

    @Override
    public String chat(String content, String user, List<Message> history) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + visionToken);

            HttpEntity<String> entity = getStringHttpEntity(content, user, history, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }

    }

    private static HttpEntity<String> getStringHttpEntity(String content, String user,List<Message> history,HttpHeaders headers) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-plus");
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 256);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", "你是代码江湖公众号的智能助手，可以为用户提供准确和专业的回答,使用简洁的文字200字内,纯文本格式输出"));

        if (history != null && !history.isEmpty()){
            Collections.reverse(history);
            for (Message message : history) {
                if (!message.getFromUser().equals(user)){
                    messages.add(new JSONObject().fluentPut("role", "assistant").fluentPut("content", message.getContent()));
                }else {
                    messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", message.getContent()));
                }
            }
        }

        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", content));
        requestBody.put("messages", messages);
        //requestBody.put("tools", new JSONArray().fluentAdd(new JSONObject().fluentPut("type", "web_search").fluentPut("web_search", new JSONObject().fluentPut("search_result", true))));

        return new HttpEntity<>(requestBody.toString(), headers);
    }

    @Override
    public String vision(String content) {
        try {
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + visionToken);

            HttpEntity<String> entity = getStringHttpEntityVision(content, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(visionServerUrl, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    private static HttpEntity<String> getStringHttpEntityVision(String content,HttpHeaders headers) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "qwen-vl-plus");
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 512);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", "你是专业的营养师，能帮助分析配料表成分"));
        JSONArray imageContent = new JSONArray();
        imageContent.add(new JSONObject().fluentPut("type", "image_url").fluentPut("image_url", new JSONObject().fluentPut("url", content)));
        imageContent.add(new JSONObject().fluentPut("type", "text").fluentPut("text", "分析图中的配料表，按照顺序列出各配料的作用，分析其中的风险"));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", imageContent));

        requestBody.put("messages", messages);

        return new HttpEntity<>(requestBody.toString(), headers);
    }
}
