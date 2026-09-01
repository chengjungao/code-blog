package com.site.blog.my.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.site.blog.my.core.entity.BlogChunk;
import com.site.blog.my.core.entity.Message;
import com.site.blog.my.core.service.ChatService;
import com.site.blog.my.core.solr.BlogSolrServer;
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

    @Autowired
    private BlogSolrServer blogSolrServer;

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
        requestBody.put("model", visionModel);
        requestBody.put("temperature", 0.1);
        requestBody.put("max_tokens", 2048);
        // GLM-5.3-Flash 强制思考模式，不支持 enable_thinking:false，改用 thinking:{level:"low"} 控制
        JSONObject thinking = new JSONObject();
        thinking.put("level", "low");
        requestBody.put("thinking", thinking);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", "你是专业的营养师，能帮助分析配料表成分"));
        JSONArray imageContent = new JSONArray();
        imageContent.add(new JSONObject().fluentPut("type", "image_url").fluentPut("image_url", new JSONObject().fluentPut("url", content)));
        imageContent.add(new JSONObject().fluentPut("type", "text").fluentPut("text", "分析图中的配料表，按照顺序列出各配料的作用，分析其中的风险"));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", imageContent));

        requestBody.put("messages", messages);

        return new HttpEntity<>(requestBody.toString(), headers);
    }

    private static final String KEYWORD_EXTRACT_PROMPT =
        "你是一个搜索关键词提取工具。根据用户问题和对话上下文，提取2-5个最适合作为搜索引擎查询的关键词。\n"
        + "规则：\n"
        + "- 提取名词、专有名词、技术术语、版本号等关键信息\n"
        + "- 去除疑问词、语气词、代词\n"
        + "- 必须保留用户原始用词和语言，禁止翻译或改写\n"
        + "- 如果是多轮对话，结合上下文消解指代（如「它」→具体名词），消解后仍用中文\n"
        + "- 输出纯关键词，用英文逗号分隔，不要其他任何文字\n"
        + "- 如果无法提取有效关键词（如纯寒暄），输出 EMPTY";

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
            // === Step 1: LLM 提取搜索关键词 ===
            String keywords = extractSearchKeywords(content, historyJson);

            // === Step 2: Solr 召回相关分块 ===
            String context = "";
            if (keywords != null) {
                context = buildContextFromSolr(keywords);
            }

            // === Step 3: 构建增强的 system prompt 并调用 LLM ===
            String enhancedPrompt = ASSISTANT_SYSTEM_PROMPT;
            if (!context.isEmpty()) {
                enhancedPrompt += "\n\n【站内文章参考】\n"
                    + "以下是网站文章中与用户问题相关的内容片段，请参考这些信息来回答：\n\n"
                    + context;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + visionToken);

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", chatModel);
            requestBody.put("temperature", 0.6);
            requestBody.put("max_tokens", 512);
            requestBody.put("enable_thinking", false);

            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", enhancedPrompt));

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

    /**
     * 调用 LLM 从用户问题中提取搜索关键词
     */
    private String extractSearchKeywords(String userQuestion, String historyJson) {
        try {
            JSONArray messages = new JSONArray();
            messages.add(new JSONObject().fluentPut("role", "system")
                .fluentPut("content", KEYWORD_EXTRACT_PROMPT));

            // 注入最近 2 轮对话帮助消解指代
            if (historyJson != null && !historyJson.trim().isEmpty()) {
                JSONArray history = JSON.parseArray(historyJson);
                int start = Math.max(0, history.size() - 4); // 最近 2 轮 = 4 条消息
                for (int i = start; i < history.size(); i++) {
                    JSONObject msg = history.getJSONObject(i);
                    messages.add(new JSONObject()
                        .fluentPut("role", msg.getString("role"))
                        .fluentPut("content", msg.getString("content")));
                }
            }

            messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", userQuestion));

            JSONObject requestBody = new JSONObject();
            requestBody.put("model", chatModel);
            requestBody.put("temperature", 0.1);
            requestBody.put("max_tokens", 64);
            requestBody.put("enable_thinking", false);
            requestBody.put("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            headers.set("Authorization", "Bearer " + visionToken);
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String keywords = JSON.parseObject(response.getBody())
                .getJSONArray("choices").getJSONObject(0)
                .getJSONObject("message").getString("content").trim();

            if ("EMPTY".equals(keywords) || keywords.isEmpty()) {
                return null;
            }
            return keywords;
        } catch (Exception e) {
            // 关键词提取失败，降级为原始问题
            return userQuestion;
        }
    }

    /**
     * 用关键词从 Solr 召回相关分块，拼接为上下文字符串
     */
    private String buildContextFromSolr(String keywords) {
        try {
            List<BlogChunk> chunks = blogSolrServer.retrieveChunks(keywords, 3);
            if (chunks.isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            for (BlogChunk chunk : chunks) {
                sb.append("【").append(chunk.getBlogTitle());
                if (chunk.getChunkTitle() != null && !chunk.getChunkTitle().isEmpty()) {
                    sb.append(" - ").append(chunk.getChunkTitle());
                }
                sb.append("】\n");
                sb.append(chunk.getChunkContent().trim());
                sb.append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
