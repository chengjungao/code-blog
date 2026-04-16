package com.site.blog.my.core.controller.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // --- 配料表识别 ---
    @PostMapping(value = "/ingredients")
    public ResponseEntity<?> analyzeIngredients(
            @RequestParam("image") MultipartFile image,@RequestHeader Map<String, String> headers) {
        if (headers.get("auth-token") == null || !headers.get("auth-token").equals(authToken)) {
            JSONObject body = new JSONObject();
            body.put("error", "未授权访问");
            return ResponseEntity.status(401).body(body);
        }
        JSONObject body = new JSONObject();
        if (image.isEmpty()) {
            body.put("error", "图片不能为空");
            return ResponseEntity.badRequest().body(body);
        }
        String result = healthService.analyzeIngredients(image);
        body.put("result", JSONArray.parseArray(result));
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/ingredientsV2")
    public ResponseEntity<?> analyzeIngredientsV2(
            @RequestParam("image") MultipartFile image,@RequestHeader Map<String, String> headers) {
        if (headers.get("auth-token") == null || !headers.get("auth-token").equals(authToken)) {
            JSONObject body = new JSONObject();
            body.put("error", "未授权访问");
            return ResponseEntity.status(401).body(body);
        }
        JSONObject body = new JSONObject();
        if (image.isEmpty()) {
            body.put("error", "图片不能为空");
            return ResponseEntity.badRequest().body(body);
        }
        String result = healthService.analyzeIngredientsV2(image);
        body.put("result", JSONObject.parseObject(result));
        return ResponseEntity.ok(body);
    }

    // --- 热量估算 ---
    @PostMapping(value = "/calories")
    public ResponseEntity<?> estimateCalories(
            @RequestParam("image") MultipartFile image,@RequestHeader Map<String, String> headers) {
        if (headers.get("auth-token") == null || !headers.get("auth-token").equals(authToken)) {
            JSONObject body = new JSONObject();
            body.put("error", "未授权访问");
            return ResponseEntity.status(401).body(body);
        }
        JSONObject body = new JSONObject();
        if (image.isEmpty()) {
            body.put("error", "图片不能为空");
            return ResponseEntity.badRequest().body(body);
        }
        String result = healthService.estimateCalories(image);
        body.put("result", JSONObject.parseObject(result));
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/ingredients_ocr")
    public ResponseEntity<?> analyzeIngredients_ocr(
            @RequestParam("image") MultipartFile image,@RequestHeader Map<String, String> headers) {
        if (headers.get("auth-token") == null || !headers.get("auth-token").equals(authToken)) {
            JSONObject body = new JSONObject();
            body.put("error", "未授权访问");
            return ResponseEntity.status(401).body(body);
        }
        JSONObject body = new JSONObject();
        if (image.isEmpty()) {
            body.put("error", "图片不能为空");
            return ResponseEntity.badRequest().body(body);
        }
        String result = healthService.analyzeIngredientsOcr(image);
        body.put("result", result);
        return ResponseEntity.ok(body);
    }

    // --- 热量估算 ---
    @PostMapping(value = "/calories_ocr")
    public ResponseEntity<?> estimateCalories_ocr(
            @RequestParam("image") MultipartFile image,@RequestHeader Map<String, String> headers) {
        if (headers.get("auth-token") == null || !headers.get("auth-token").equals(authToken)) {
            JSONObject body = new JSONObject();
            body.put("error", "未授权访问");
            return ResponseEntity.status(401).body(body);
        }
        JSONObject body = new JSONObject();
        if (image.isEmpty()) {
            body.put("error", "图片不能为空");
            return ResponseEntity.badRequest().body(body);
        }
        String result = healthService.estimateCaloriesOcr(image);
        body.put("result", result);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/ingredientsV3")
    public ResponseEntity<?> analyzeIngredientsV3(@RequestBody Map<String,String> request, @RequestHeader Map<String, String> headers) {
        if (headers.get("auth-token") == null || !headers.get("auth-token").equals(authToken)) {
            JSONObject body = new JSONObject();
            body.put("error", "未授权访问");
            return ResponseEntity.status(401).body(body);
        }
        JSONObject body = new JSONObject();
        String content = request.get("content");
        if (content == null || content.isEmpty()) {
            body.put("error", "内容不能为空");
            return ResponseEntity.badRequest().body(body);
        }
        String result = healthService.analyzeIngredientsV3(content);
        body.put("result", JSONObject.parseObject(result));
        return ResponseEntity.ok(body);
    }

    // --- 热量估算 ---
    @PostMapping(value = "/caloriesV3")
    public ResponseEntity<?> estimateCaloriesV3(
            @RequestBody Map<String,String> request,@RequestHeader Map<String, String> headers) {
        if (headers.get("auth-token") == null || !headers.get("auth-token").equals(authToken)) {
            JSONObject body = new JSONObject();
            body.put("error", "未授权访问");
            return ResponseEntity.status(401).body(body);
        }
        JSONObject body = new JSONObject();
        String content = request.get("content");
        if (content == null || content.isEmpty()) {
            body.put("error", "内容不能为空");
            return ResponseEntity.badRequest().body(body);
        }
        String result = healthService.estimateCaloriesV3(content);
        body.put("result", JSONObject.parseObject(result));
        return ResponseEntity.ok(body);
    }

    // --- 辅助方法：尝试解析 JSON，失败则返回字符串 ---
    private Object parseJsonOrString(String str) {
        try {
            return new ObjectMapper().readTree(str);
        } catch (Exception e) {
            return str; // 原始文本返回
        }
    }
}
