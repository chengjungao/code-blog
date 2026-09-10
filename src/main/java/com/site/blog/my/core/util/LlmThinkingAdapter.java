package com.site.blog.my.core.util;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 大模型「思考开关」参数适配器。
 *
 * 各供应商关闭深度思考的字段名 / 结构不同，这里收敛成一处，由配置决定，
 * 换模型只需改配置，不再改代码。
 *
 * 支持的 style 取值（不区分大小写）：
 *   ark         -> {"thinking":{"type":"disabled"}}     火山方舟 豆包 Seed / DeepSeek 等
 *   glm-level   -> {"thinking":{"level":"low"}}          智谱 GLM 强制思考型模型（如 glm-5.3-flash，不支持 enable_thinking）
 *   qwen        -> {"enable_thinking":false}             阿里 DashScope Qwen 系
 *   glm         -> {"enable_thinking":false}             智谱 GLM 文本模型（如 glm-5.2）
 *   none        -> 不传该字段，走模型默认
 *
 * 配置示例：
 *   llm.thinking-style: ark
 *   llm.vision-thinking-style: ark      # 不配置则回退到 thinking-style
 */
@Component
public class LlmThinkingAdapter {

    private static final Logger log = LoggerFactory.getLogger(LlmThinkingAdapter.class);

    public static final String ARK = "ark";
    public static final String GLM_LEVEL = "glm-level";
    public static final String QWEN = "qwen";
    public static final String GLM = "glm";
    public static final String NONE = "none";

    /** 文本请求的思考开关风格 */
    @Value("${llm.thinking-style:ark}")
    private String thinkingStyle;

    /** 视觉请求的思考开关风格，留空则回退到 thinking-style */
    @Value("${llm.vision-thinking-style:}")
    private String visionThinkingStyle;

    /** 文本请求：按配置写入思考控制字段 */
    public void applyText(JSONObject body) {
        apply(body, thinkingStyle);
    }

    /** 视觉请求：优先 vision-thinking-style，未配置则回退 thinking-style */
    public void applyVision(JSONObject body) {
        String style = isBlank(visionThinkingStyle) ? thinkingStyle : visionThinkingStyle;
        apply(body, style);
    }

    private void apply(JSONObject body, String style) {
        if (isBlank(style)) {
            return;
        }
        String s = style.trim().toLowerCase();
        switch (s) {
            case ARK:
                body.put("thinking", new JSONObject().fluentPut("type", "disabled"));
                break;
            case GLM_LEVEL:
                body.put("thinking", new JSONObject().fluentPut("level", "low"));
                break;
            case QWEN:
            case GLM:
                body.put("enable_thinking", false);
                break;
            case NONE:
                break;
            default:
                log.warn("未知的 llm.thinking-style 配置值：{}，本次不附加思考控制字段（支持：ark/glm-level/qwen/glm/none）", style);
                break;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
