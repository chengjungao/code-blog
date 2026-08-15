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

    @Value("${vision.server.url:}")
    private String visionServerUrl;

    @Value("${vision.token:}")
    private String visionToken;

    @Value("${vision.model:}")
    private String visionModel;

    @Value("${chat.model:}")
    private String chatModel;

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

    private HttpEntity<String> getStringHttpEntity(String content, String user,List<Message> history,HttpHeaders headers) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", chatModel);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 256);
        requestBody.put("enable_thinking",false);
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

    private HttpEntity<String> getStringHttpEntityVision(String content,HttpHeaders headers) {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", chatModel);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 512);
        requestBody.put("enable_thinking",false);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", "你是专业的营养师，能帮助分析配料表成分"));
        JSONArray imageContent = new JSONArray();
        imageContent.add(new JSONObject().fluentPut("type", "image_url").fluentPut("image_url", new JSONObject().fluentPut("url", content)));
        imageContent.add(new JSONObject().fluentPut("type", "text").fluentPut("text", "分析图中的配料表，按照顺序列出各配料的作用，分析其中的风险"));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", imageContent));

        requestBody.put("messages", messages);

        return new HttpEntity<>(requestBody.toString(), headers);
    }

    private static final String ASSISTANT_SYSTEM_PROMPT =
        "你是程军高的智能分身，在「拾光集」个人网站上与访客对话。你就是程军高本人。\n\n"
        + "【关于程军高】\n"
        + "- 搜索架构师 / AI 应用架构师，11 年后端开发与架构经验，6 年团队管理经验\n"
        + "- 在某电商公司主导搜索平台五轮架构演进（单体→分布式→微服务→双塔+多路召回→AI Search），日均 PV 1500 万，覆盖 Solr 引擎、索引系统、召回与排序、Query 理解\n"
        + "- 近一年深耕 Agent 与 MCP 平台：主导 Search Agent（Gemma 微调、LangGraph ReAct 编排、GPT、DSPy）与 MCP 平台建设（7000+ API 工具化封装）\n"
        + "- 技术栈：Java/Scala/Python, Spring Cloud, Hadoop/Spark/Flink/Kafka, Solr/ES/Milvus, LangGraph, Docker/K8S\n"
        + "- 持有 PMP 证书，担任过 Scrum Master\n"
        + "- 坐标上海，邮箱 chengjungao@foxmail.com\n\n"
        + "【对话规则】\n"
        + "- 始终以第一人称回答，你就是程军高\n"
        + "- 语气专业但随和，像和朋友喝咖啡时聊天\n"
        + "- 回答控制在 300 字以内，简洁有料\n"
        + "- 涉及雇主时用「某电商公司」代称，不透露具体公司名\n"
        + "- 技术问题可以适度展开，但不编造没有的经历\n"
        + "- 不确定或不知道的事直接说不知道，不要瞎编\n"
        + "- 需要联系时引导到邮箱 chengjungao@foxmail.com\n"
        + "- 用纯文本回答，不要使用 markdown 格式标记";

    @Override
    public String assistantChat(String content, String historyJson) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + visionToken);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", chatModel);
            requestBody.put("temperature", 0.6);
            requestBody.put("max_tokens", 512);
            requestBody.put("enable_thinking", false);

            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", ASSISTANT_SYSTEM_PROMPT));

            if (historyJson != null && !historyJson.trim().isEmpty()) {
                JSONArray history = JSON.parseArray(historyJson);
                for (int i = 0; i < history.size(); i++) {
                    JSONObject msg = history.getJSONObject(i);
                    messages.add(new JSONObject()
                        .fluentPut("role", msg.getString("role"))
                        .fluentPut("content", msg.getString("content")));
                }
            }

            messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", content));
            requestBody.put("messages", messages);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        } catch (Exception e) {
            return "分身暂时开小差了，稍后再试一下吧～";
        }
    }
}
