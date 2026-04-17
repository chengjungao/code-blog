package com.site.blog.my.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.site.blog.my.core.entity.Message;
import com.site.blog.my.core.service.HealthService;
import com.site.blog.my.core.util.PromptUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

@Service
public class HealthServiceImpl implements HealthService {
    @Autowired
    private RestTemplate restTemplate;

    @Value("${vision.server.url:}")
    private String visionServerUrl;

    @Value("${chat.server.url:}")
    private String url;

    @Value("${vision.token:}")
    private String visionToken;

    @Value("${vision.model:}")
    private String visionModel;

    @Value("${chat.model:}")
    private String chatModel;

    @Override
    public String analyzeIngredients(MultipartFile image) {
        try {
            String prompt ="请分析这张食品配料表图片： \n 1. 提取所有配料名称 \n 2. 对每个配料说明其常见用途（如防腐剂、增稠剂等） \n 3. 给出每人每日建议摄入量 \n 4. 标注是否有潜在健康风险 \n " +
                    "请以 JSON 数组格式返回，不要额外解释： [ \"ingredient\": \"苯甲酸钠\", \"purpose\": \"防腐剂\", \"recommended_intake\": \"≤5mg/kg体重/天\", \"risk\": \"过量可能引起过敏\"}] ";
            HttpEntity<String> entity = getStringHttpEntityVision(image,prompt);
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(visionServerUrl, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    @Override
    public String analyzeIngredientsV2(MultipartFile image) {
        try {

            HttpEntity<String> entity = getStringHttpEntityVision(image, PromptUtils.INGREDIENTS_PROMPT);
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(visionServerUrl, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return extractJsonContent(jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    @Override
    public String estimateCalories(MultipartFile image) {
        try {
            HttpEntity<String> entity = getStringHttpEntityVision(image,PromptUtils.CALORIES_PROMPT);
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(visionServerUrl, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return extractJsonContent(jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    @Override
    public String analyzeIngredientsOcr(MultipartFile image) {
        try {

            HttpEntity<String> entity = getStringHttpEntityVision(image, PromptUtils.INGREDIENTS_ORC_PROMPT);
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(visionServerUrl, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return extractJsonContent(jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    @Override
    public String estimateCaloriesOcr(MultipartFile image) {
        try {

            HttpEntity<String> entity = getStringHttpEntityVision(image, PromptUtils.CALORIES_ORC_PROMPT);
            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(visionServerUrl, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return extractJsonContent(jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    @Override
    public String analyzeIngredientsV3(String content) {
        try {

            HttpEntity<String> entity = getStringHttpEntityText(content, PromptUtils.INGREDIENTS_TXT_PROMPT);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return extractJsonContent(jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    @Override
    public String estimateCaloriesV3(String content) {
        try {
            HttpEntity<String> entity = getStringHttpEntityText(content, PromptUtils.CALORIES_TXT_PROMPT);

            // 发送请求
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JSONObject jsonObject = JSON.parseObject(response.getBody());
            return extractJsonContent(jsonObject.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));

        }catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    private HttpEntity<String> getStringHttpEntityVision(MultipartFile image,String systemPrompt) throws Exception {
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + visionToken);

        String base64_image = java.util.Base64.getEncoder().encodeToString(image.getBytes());
        String content = "data:" + image.getContentType() + ";base64," + base64_image;
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", visionModel);
        requestBody.put("enable_thinking",false);
        requestBody.put("temperature", 0);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", systemPrompt));
        JSONArray imageContent = new JSONArray();
        imageContent.add(new JSONObject().fluentPut("type","image_url").fluentPut("image_url",
                new JSONObject().fluentPut("url",content)));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", imageContent));

        requestBody.put("messages", messages);
        return new HttpEntity<>(requestBody.toString(), headers);
    }

    private HttpEntity<String> getStringHttpEntityText(String userInput,String systemPrompt) throws Exception {
        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + visionToken);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", chatModel);
        requestBody.put("temperature", 0);
        requestBody.put("enable_thinking",false);
        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", systemPrompt));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", userInput));
        requestBody.put("messages", messages);
        return new HttpEntity<>(requestBody.toString(), headers);
    }

    public String extractJsonContent(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "{}";
        }

        // 匹配 ```json\n 和 ``` 之间的内容
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("```(json|JSON)?\\s*\\n?(.*?)\\n?```", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(rawResponse.trim());

        if (matcher.find()) {
            return matcher.group(2).trim(); // 返回中间的 JSON 字符串
        }

        // 如果没有 ``` 包裹，尝试直接解析
        try {
            // 简单验证是否是合法 JSON
            return rawResponse.trim();
        } catch (Exception e) {
            // 都不行，返回空对象或原内容
            return rawResponse.trim(); // 或返回 "{}"
        }
    }
}
