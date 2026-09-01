package com.site.blog.my.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

@Service
public class HealthServiceImpl implements HealthService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${vision.server.url:}")
    private String visionServerUrl;

    @Value("${chat.server.url:}")
    private String chatUrl;

    @Value("${vision.token:}")
    private String apiToken;

    @Value("${vision.model:}")
    private String visionModel;

    @Value("${chat.model:}")
    private String chatModel;

    // ==================== 配料表 OCR ====================

    @Override
    public String analyzeIngredientsOcr(MultipartFile image) {
        try {
            HttpEntity<String> entity = buildVisionRequest(image, PromptUtils.INGREDIENTS_ORC_PROMPT);
            ResponseEntity<String> response = restTemplate.exchange(visionServerUrl, HttpMethod.POST, entity, String.class);
            JSONObject json = JSON.parseObject(response.getBody());
            return extractJsonContent(json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));
        } catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    // ==================== 配料表文本分析 ====================

    @Override
    public String analyzeIngredientsV3(String content) {
        try {
            HttpEntity<String> entity = buildTextRequest(content, PromptUtils.INGREDIENTS_TXT_PROMPT);
            ResponseEntity<String> response = restTemplate.exchange(chatUrl, HttpMethod.POST, entity, String.class);
            JSONObject json = JSON.parseObject(response.getBody());
            return extractJsonContent(json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));
        } catch (Exception e) {
            return "助手暂时出现故障，无法响应您的问题！";
        }
    }

    // ==================== 配料知识查询 ====================

    @Override
    public String lookupIngredient(String ingredientName) {
        try {
            String prompt = PromptUtils.INGREDIENT_LOOKUP_PROMPT + "\n\n用户查询：" + ingredientName;
            HttpEntity<String> entity = buildTextRequest(ingredientName, prompt);
            ResponseEntity<String> response = restTemplate.exchange(chatUrl, HttpMethod.POST, entity, String.class);
            JSONObject json = JSON.parseObject(response.getBody());
            return extractJsonContent(json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content"));
        } catch (Exception e) {
            return "{\"error\": \"查询失败，请稍后重试\"}";
        }
    }

    // ==================== 请求构建 ====================

    private HttpEntity<String> buildVisionRequest(MultipartFile image, String systemPrompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + apiToken);

        String base64 = java.util.Base64.getEncoder().encodeToString(image.getBytes());
        String imageUrl = "data:" + image.getContentType() + ";base64," + base64;

        JSONObject body = new JSONObject();
        body.put("model", visionModel);
        // GLM-5.3-Flash 强制思考模式，不支持 enable_thinking:false，改用 thinking:{level:"low"} 控制
        JSONObject thinking = new JSONObject();
        thinking.put("level", "low");
        body.put("thinking", thinking);
        body.put("temperature", 0);
        body.put("max_tokens", 2048);

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", systemPrompt));
        JSONArray imageContent = new JSONArray();
        imageContent.add(new JSONObject().fluentPut("type", "image_url").fluentPut("image_url",
                new JSONObject().fluentPut("url", imageUrl)));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", imageContent));
        body.put("messages", messages);

        return new HttpEntity<>(body.toString(), headers);
    }

    private HttpEntity<String> buildTextRequest(String userInput, String systemPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer " + apiToken);

        JSONObject body = new JSONObject();
        body.put("model", chatModel);
        body.put("temperature", 0);
        body.put("enable_thinking", false);

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", systemPrompt));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", userInput));
        body.put("messages", messages);

        return new HttpEntity<>(body.toString(), headers);
    }

    // ==================== JSON 提取 ====================

    public String extractJsonContent(String rawResponse) {
        if (rawResponse == null || rawResponse.trim().isEmpty()) {
            return "{}";
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "```(json|JSON)?\\s*\\n?(.*?)\\n?```", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(rawResponse.trim());
        if (matcher.find()) {
            return matcher.group(2).trim();
        }
        return rawResponse.trim();
    }
}
