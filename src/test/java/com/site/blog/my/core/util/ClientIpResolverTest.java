package com.site.blog.my.core.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端 IP 解析。链路：浏览器 → 阿里云 CDN → 源站 nginx → Spring Boot。
 *
 * <p>这些用例锁定的是一条容易踩的坑：nginx 用 {@code $proxy_add_x_forwarded_for}
 * 组装 XFF 时会把<b>直连方</b>（= CDN 边缘节点）追加到<b>末尾</b>，
 * 所以链尾不是访客；而链首是客户端可伪造的，也不能采信。
 */
class ClientIpResolverTest {

    @Test
    void 剥掉末尾CDN节点后取到真实访客() {
        ClientIpResolver.Resolved r = ClientIpResolver.resolve(
                "1.2.3.4, 47.100.9.9", "47.100.9.9", "172.17.0.5");
        assertEquals("1.2.3.4", r.getIp());
        assertEquals("xff", r.getSource());
        assertFalse(r.isInternal());
    }

    @Test
    void 客户端伪造XFF前缀时只采信链尾() {
        // 攻击者自造 9.9.9.9 想伪装身份；真实访客是 CDN 写入的 1.2.3.4
        ClientIpResolver.Resolved r = ClientIpResolver.resolve(
                "9.9.9.9, 1.2.3.4, 47.100.9.9", null, "172.17.0.5");
        assertEquals("1.2.3.4", r.getIp());
    }

    @Test
    void 同一客户端伪造不同前缀不会改变解析结果() {
        String a = ClientIpResolver.resolve("9.9.9.9, 1.2.3.4, 47.100.9.9", null, "172.17.0.5").getIp();
        String b = ClientIpResolver.resolve("8.8.8.8, 1.2.3.4, 47.100.9.9", null, "172.17.0.5").getIp();
        assertEquals(a, b, "伪造前缀不应影响限流/UV 口径");
    }

    @Test
    void 内网来源标记为internal() {
        ClientIpResolver.Resolved r = ClientIpResolver.resolve("127.0.0.1", "127.0.0.1", "172.17.0.1");
        assertEquals("127.0.0.1", r.getIp());
        assertEquals("xff-private", r.getSource());
        assertTrue(r.isInternal(), "预渲染走容器内 nginx，必须能被识别为内网流量");
    }

    @Test
    void 无XFF时退回XRealIP再退回socket地址() {
        ClientIpResolver.Resolved onlyRealIp = ClientIpResolver.resolve(null, "1.2.3.4", "172.17.0.1");
        assertEquals("1.2.3.4", onlyRealIp.getIp());
        assertEquals("realip", onlyRealIp.getSource());

        ClientIpResolver.Resolved none = ClientIpResolver.resolve(null, null, "172.17.0.1");
        assertEquals("172.17.0.1", none.getIp());
        assertEquals("remote", none.getSource());
        assertTrue(none.isInternal());
    }

    @Test
    void unknown占位值被跳过() {
        ClientIpResolver.Resolved r = ClientIpResolver.resolve("unknown", "1.2.3.4", "10.0.0.1");
        assertEquals("1.2.3.4", r.getIp());
    }

    @Test
    void 原始XFF被完整保留用于事后核对() {
        String raw = "9.9.9.9, 1.2.3.4, 47.100.9.9";
        ClientIpResolver.Resolved r = ClientIpResolver.resolve(raw, null, "172.17.0.5");
        assertEquals(raw, r.getRawXff());
    }

    @Test
    void 公网地址判定() {
        assertTrue(ClientIpResolver.isPublic("8.8.8.8"));
        assertTrue(ClientIpResolver.isPublic("223.5.5.5"));
        assertTrue(ClientIpResolver.isPublic("172.32.0.1"), "172.32 已不在 172.16/12 私网段内");
        assertFalse(ClientIpResolver.isPublic("10.0.0.1"));
        assertFalse(ClientIpResolver.isPublic("127.0.0.1"));
        assertFalse(ClientIpResolver.isPublic("192.168.1.1"));
        assertFalse(ClientIpResolver.isPublic("172.16.0.1"));
        assertFalse(ClientIpResolver.isPublic("169.254.1.1"));
        assertFalse(ClientIpResolver.isPublic("100.64.0.1"));
        assertFalse(ClientIpResolver.isPublic("224.0.0.1"));
        assertFalse(ClientIpResolver.isPublic("unknown"));
        assertFalse(ClientIpResolver.isPublic(null));
        assertFalse(ClientIpResolver.isPublic("::1"));
        assertFalse(ClientIpResolver.isPublic("fe80::1"));
        assertTrue(ClientIpResolver.isPublic("2400:3200::1"));
    }
}
