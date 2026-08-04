package com.site.blog.my.core.controller.common;

import com.alibaba.fastjson.JSONObject;
import com.site.blog.my.core.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api")
public class HealthAgentController {

    @Autowired
    private HealthService healthService;

    @Value("${token:}")
    private String authToken;

    // ==================== 配料表识别 ====================

    @PostMapping(value = "/ingredients_ocr")
    public ResponseEntity<?> analyzeIngredientsOcr(
            @RequestParam("image") MultipartFile image,
            @RequestHeader Map<String, String> headers) {
        if (!checkAuth(headers)) {
            return ResponseEntity.status(401).body(msg("error", "未授权访问"));
        }
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(msg("error", "图片不能为空"));
        }
        String result = healthService.analyzeIngredientsOcr(image);
        return ResponseEntity.ok(msg("result", result));
    }

    @PostMapping(value = "/ingredientsV3")
    public ResponseEntity<?> analyzeIngredientsV3(
            @RequestBody Map<String, String> request,
            @RequestHeader Map<String, String> headers) {
        if (!checkAuth(headers)) {
            return ResponseEntity.status(401).body(msg("error", "未授权访问"));
        }
        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(msg("error", "内容不能为空"));
        }
        String result = healthService.analyzeIngredientsV3(content.trim());
        return ResponseEntity.ok(msg("result", JSONObject.parseObject(result)));
    }

    // ==================== 配料知识查询 ====================

    @PostMapping(value = "/ingredients/lookup")
    public ResponseEntity<?> lookupIngredient(
            @RequestBody Map<String, String> request,
            @RequestHeader Map<String, String> headers) {
        if (!checkAuth(headers)) {
            return ResponseEntity.status(401).body(msg("error", "未授权访问"));
        }
        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(msg("error", "请输入配料名称"));
        }
        String result = healthService.lookupIngredient(name.trim());
        return ResponseEntity.ok(msg("result", JSONObject.parseObject(result)));
    }

    // ==================== 工具方法 ====================

    private boolean checkAuth(Map<String, String> headers) {
        String token = headers.get("auth-token");
        return token != null && token.equals(authToken);
    }

    private Map<String, Object> msg(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

}
