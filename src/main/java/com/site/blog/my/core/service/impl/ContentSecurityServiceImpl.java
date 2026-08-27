package com.site.blog.my.core.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.site.blog.my.core.service.ContentSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 抖音内容安全检测实现。
 *
 * 流程：先用 AppID + AppSecret 获取 access_token（服务端缓存 2h，提前 5min 失效），
 * 再调用文本 / 图片检测接口。检测接口异常时默认「fail-closed」（拦截），
 * 可通过 douyin.security.fail-open-on-error=true 切换为放行（避免抖音侧抖动影响业务）。
 */
@Service
public class ContentSecurityServiceImpl implements ContentSecurityService {

    private static final Logger log = LoggerFactory.getLogger(ContentSecurityServiceImpl.class);

    private static final String TOKEN_PATH = "/api/apps/v2/token";
    private static final String TEXT_PATH = "/api/v2/tags/text/antidirt";
    private static final String IMAGE_PATH = "/api/apps/censor/image";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${douyin.app-id:}")
    private String appId;

    @Value("${douyin.app-secret:}")
    private String appSecret;

    @Value("${douyin.api-base:https://developer.toutiao.com}")
    private String apiBase;

    @Value("${douyin.security.fail-open-on-error:false}")
    private boolean failOpenOnError;

    // ---------- access_token 缓存 ----------
    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private volatile long tokenExpireAt = 0L;

    // ==================== 文本检测 ====================

    @Override
    public SecurityCheckResult checkText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return SecurityCheckResult.safe();
        }
        try {
            String token = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Token", token);

            JSONObject body = new JSONObject();
            JSONArray tasks = new JSONArray();
            tasks.add(new JSONObject().fluentPut("content", text));
            body.put("tasks", tasks);

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBase + TEXT_PATH, HttpMethod.POST, entity, String.class);
            return parseTextResult(response.getBody());
        } catch (Exception e) {
            log.error("文本安全检测异常", e);
            return handleError();
        }
    }

    private SecurityCheckResult parseTextResult(String body) {
        JSONObject json = JSON.parseObject(body);
        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) {
            // 整个请求级失败（如 401 token 失效），结构里没有 data 数组
            return handleError();
        }
        JSONObject item = data.getJSONObject(0);
        if (item.getIntValue("code") != 0) {
            return handleError();
        }
        JSONArray predicts = item.getJSONArray("predicts");
        if (predicts != null) {
            StringBuilder hits = new StringBuilder();
            for (int i = 0; i < predicts.size(); i++) {
                JSONObject p = predicts.getJSONObject(i);
                if (p.getBooleanValue("hit")) {
                    hits.append(p.getString("model_name")).append(";");
                }
            }
            if (hits.length() > 0) {
                return SecurityCheckResult.blocked("文本命中违规标签: " + hits);
            }
        }
        return SecurityCheckResult.safe();
    }

    // ==================== 图片检测 ====================

    @Override
    public SecurityCheckResult checkImage(byte[] imageBytes, String contentType) {
        if (imageBytes == null || imageBytes.length == 0) {
            return SecurityCheckResult.safe();
        }
        try {
            String token = getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            JSONObject body = new JSONObject();
            body.put("app_id", appId);
            body.put("access_token", token);
            body.put("image_data", Base64.getEncoder().encodeToString(imageBytes));

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBase + IMAGE_PATH, HttpMethod.POST, entity, String.class);
            return parseImageResult(response.getBody());
        } catch (Exception e) {
            log.error("图片安全检测异常, contentType={}", contentType, e);
            return handleError();
        }
    }

    private SecurityCheckResult parseImageResult(String body) {
        JSONObject json = JSON.parseObject(body);
        int error = json.getIntValue("error");
        if (error != 0) {
            return handleError();
        }
        JSONArray predicts = json.getJSONArray("predicts");
        if (predicts != null) {
            StringBuilder hits = new StringBuilder();
            for (int i = 0; i < predicts.size(); i++) {
                JSONObject p = predicts.getJSONObject(i);
                if (p.getBooleanValue("hit")) {
                    hits.append(p.getString("model_name")).append(",");
                }
            }
            if (hits.length() > 0) {
                return SecurityCheckResult.blocked("图片命中违规标签: " + hits);
            }
        }
        return SecurityCheckResult.safe();
    }

    // ==================== access_token ====================

    private String getAccessToken() {
        if (cachedToken.get() != null && System.currentTimeMillis() < tokenExpireAt) {
            return cachedToken.get();
        }
        synchronized (this) {
            // 双重检查，避免并发重复刷新
            if (cachedToken.get() != null && System.currentTimeMillis() < tokenExpireAt) {
                return cachedToken.get();
            }
            return fetchAccessToken();
        }
    }

    private String fetchAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject body = new JSONObject();
        body.put("appid", appId);
        body.put("secret", appSecret);
        body.put("grant_type", "client_credential");

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiBase + TOKEN_PATH, HttpMethod.POST, entity, String.class);
            JSONObject json = JSON.parseObject(response.getBody());
            if (json.getIntValue("err_no") != 0) {
                throw new IllegalStateException("获取 access_token 失败: err_no=" + json.getIntValue("err_no")
                        + ", tips=" + json.getString("err_tips"));
            }
            JSONObject data = json.getJSONObject("data");
            String token = data.getString("access_token");
            long expiresIn = data.getLongValue("expires_in");
            // 提前 5 分钟失效，避免临界时刻用到过期 token（重复获取会把旧 token 缩短到 5min）
            long effective = (expiresIn > 300 ? expiresIn - 300 : expiresIn) * 1000L;
            tokenExpireAt = System.currentTimeMillis() + effective;
            cachedToken.set(token);
            return token;
        } catch (RestClientException e) {
            throw new IllegalStateException("获取 access_token 网络异常", e);
        }
    }

    // ==================== 错误处理策略 ====================

    private SecurityCheckResult handleError() {
        if (failOpenOnError) {
            log.warn("内容安全检测服务异常，按 fail-open 配置放行");
            return SecurityCheckResult.safe();
        }
        return SecurityCheckResult.blocked("内容安全检测服务暂时不可用，请稍后再试");
    }
}
