package com.site.blog.my.core.service;

/**
 * 抖音开放平台内容安全检测（文本 / 图片）。
 *
 * 参考：
 *  - 文本检测  https://developer.open-douyin.com/docs/resource/zh-CN/mini-app/develop/server/basic-abilities/content-security/content-security-detect
 *  - 图片检测V2 https://developer.open-douyin.com/docs/resource/zh-CN/mini-app/develop/server/basic-abilities/content-security/picture-detect-v2
 */
public interface ContentSecurityService {

    /**
     * 检测文本是否包含违法违规内容。
     *
     * @param text 待检测文本（null / 空串视为安全）
     * @return 检测结果，{@link SecurityCheckResult#isSafe()} 为 true 时表示可放行
     */
    SecurityCheckResult checkText(String text);

    /**
     * 检测图片是否包含违法违规内容（传入原始字节，内部转 base64）。
     *
     * @param imageBytes    图片原始字节（null / 空视为安全）
     * @param contentType   图片 MIME，仅用于日志，可为 null
     * @return 检测结果
     */
    SecurityCheckResult checkImage(byte[] imageBytes, String contentType);

    /**
     * 检测结果。safe=true 表示未命中违规标签，可放行。
     */
    class SecurityCheckResult {
        private final boolean safe;
        private final String detail;

        private SecurityCheckResult(boolean safe, String detail) {
            this.safe = safe;
            this.detail = detail == null ? "" : detail;
        }

        public static SecurityCheckResult safe() {
            return new SecurityCheckResult(true, "");
        }

        public static SecurityCheckResult blocked(String detail) {
            return new SecurityCheckResult(false, detail);
        }

        public boolean isSafe() {
            return safe;
        }

        public String getDetail() {
            return detail;
        }
    }
}
