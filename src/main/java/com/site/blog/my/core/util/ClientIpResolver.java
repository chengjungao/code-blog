package com.site.blog.my.core.util;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户端真实 IP 解析（站点链路：浏览器 → 阿里云 CDN → 源站 nginx → Spring Boot）。
 *
 * <p>四条决定解析顺序的链路事实：
 * <ol>
 *   <li>nginx 用 {@code $proxy_add_x_forwarded_for} 组装 XFF，会在<b>末尾追加直连方地址</b>；
 *       而直连方是 CDN 边缘节点 ⇒ XFF 最后一段不是访客 IP，必须先剥掉。</li>
 *   <li>nginx 的 {@code X-Real-IP} 被设成 {@code $remote_addr}，同样是 CDN 节点 IP ⇒ 只能兜底。</li>
 *   <li>CDN 会把访客真实 IP 写在自己那一段（即剥掉末段后，链尾第一个非保留地址）。</li>
 *   <li>客户端可自行伪造 XFF 前缀 ⇒ <b>只能采信链尾，不能采信链首</b>
 *       （旧实现取首段，UV 可被任意伪造）。</li>
 * </ol>
 *
 * <p>原始 X-Forwarded-For 会随解析结果一起返回，并写入访问明细日志，用于事后核对
 * CDN 到底传了什么——这是「统计准不准」唯一可自证的手段。
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * 解析结果。
     */
    public static class Resolved {
        private final String ip;
        private final String source;
        private final String rawXff;

        public Resolved(String ip, String source, String rawXff) {
            this.ip = ip;
            this.source = source;
            this.rawXff = rawXff;
        }

        /** 解析出的客户端 IP */
        public String getIp() {
            return ip;
        }

        /** 取值来源：xff / xff-private / realip / remote */
        public String getSource() {
            return source;
        }

        /** 原始 X-Forwarded-For（审计用） */
        public String getRawXff() {
            return rawXff;
        }

        /** 是否内网/本机来源（预渲染、内网直连等，不计入报表） */
        public boolean isInternal() {
            return !isPublic(ip);
        }
    }

    public static Resolved resolve(HttpServletRequest request) {
        if (request == null) {
            return new Resolved("unknown", "none", "");
        }
        return resolve(request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP"),
                request.getRemoteAddr());
    }

    public static Resolved resolve(String xff, String xRealIp, String remoteAddr) {
        String raw = xff == null ? "" : xff.trim();
        List<String> chain = new ArrayList<String>();
        if (!raw.isEmpty()) {
            for (String part : raw.split(",")) {
                String s = part.trim();
                if (!s.isEmpty() && !"unknown".equalsIgnoreCase(s)) {
                    chain.add(s);
                }
            }
        }
        // ① 剥掉 nginx 追加的直连方地址（CDN 边缘节点）
        if (chain.size() >= 2) {
            chain.remove(chain.size() - 1);
        }
        // ② 从链尾往前找第一个公网地址（CDN 写入的真实访客 IP）
        for (int i = chain.size() - 1; i >= 0; i--) {
            String candidate = chain.get(i);
            if (isPublic(candidate)) {
                return new Resolved(candidate, "xff", raw);
            }
        }
        // ③ 链内全是保留地址（本机预渲染 / 内网直连）：保留原值，标记为 internal
        if (!chain.isEmpty()) {
            return new Resolved(chain.get(chain.size() - 1), "xff-private", raw);
        }
        // ④ XFF 缺失：退回 X-Real-IP，再退回 socket 地址
        if (xRealIp != null && !xRealIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(xRealIp.trim())) {
            return new Resolved(xRealIp.trim(), "realip", raw);
        }
        String remote = (remoteAddr == null || remoteAddr.isEmpty()) ? "unknown" : remoteAddr;
        return new Resolved(remote, "remote", raw);
    }

    /**
     * 是否公网地址。回环、私网、链路本地、运营商级 NAT、组播/保留段一律视为非公网。
     */
    public static boolean isPublic(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        if (ip.indexOf(':') >= 0) {
            String l = ip.toLowerCase();
            return !(l.startsWith("::1") || l.startsWith("fc") || l.startsWith("fd") || l.startsWith("fe80"));
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        int[] n = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                n[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                return false;
            }
            if (n[i] < 0 || n[i] > 255) {
                return false;
            }
        }
        if (n[0] == 10 || n[0] == 127 || n[0] == 0) {
            return false;
        }
        if (n[0] == 172 && n[1] >= 16 && n[1] <= 31) {
            return false;
        }
        if (n[0] == 192 && n[1] == 168) {
            return false;
        }
        if (n[0] == 169 && n[1] == 254) {
            return false;
        }
        if (n[0] == 100 && n[1] >= 64 && n[1] <= 127) {
            return false;
        }
        // 224+ 组播与保留
        return n[0] < 224;
    }
}
