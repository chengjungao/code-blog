package com.site.blog.my.core.util;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * 敏感词过滤工具。
 * 基于 DFA（确定有限自动机）算法实现高效多模式匹配。
 */
@Component
public class SensitiveWordFilter {

    private static final String END = "END";

    private final Map<String, Map> dfaMap = new HashMap<>();

    /**
     * 默认敏感词库（可后续扩展为从文件或数据库加载）
     */
    private static final Set<String> DEFAULT_WORDS = new HashSet<>(Arrays.asList(
            // 政治类
            "反动", "颠覆", "暴政",
            // 色情类
            "色情", "黄赌毒", "裸聊", "一夜情", " AV ", "成人电影",
            // 暴力类
            "杀人", "自杀", "炸弹", "爆炸物", "恐怖袭击",
            // 辱骂类
            "傻逼", "操你", "滚蛋", "去死", "废物", "脑残", "弱智",
            // 广告类
            "加微信", "加QQ", "免费送", "代刷", "刷单", "兼职日入",
            // 其他
            "赌博", "博彩", "时时彩", "六合彩", "传销", "诈骗"
    ));

    @PostConstruct
    public void init() {
        for (String word : DEFAULT_WORDS) {
            addWord(word);
        }
    }

    /**
     * 添加敏感词到 DFA 树
     */
    @SuppressWarnings("unchecked")
    private void addWord(String word) {
        if (word == null || word.trim().isEmpty()) return;
        word = word.trim();
        Map<String, Map> current = dfaMap;
        for (int i = 0; i < word.length(); i++) {
            String ch = String.valueOf(word.charAt(i));
            Map<String, Map> next = current.get(ch);
            if (next == null) {
                next = new HashMap<>();
                current.put(ch, next);
            }
            current = next;
            if (i == word.length() - 1) {
                current.put(END, null);
            }
        }
    }

    /**
     * 检查文本中是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) {
            if (checkFrom(text, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 过滤敏感词，替换为 ***
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) return text;
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int end = findEnd(text, i);
            if (end > i) {
                result.append("***");
                i = end + 1;
            } else {
                result.append(text.charAt(i));
                i++;
            }
        }
        return result.toString();
    }

    /**
     * 获取文本中匹配到的敏感词列表
     */
    public Set<String> getSensitiveWords(String text) {
        Set<String> words = new LinkedHashSet<>();
        if (text == null || text.isEmpty()) return words;
        for (int i = 0; i < text.length(); i++) {
            int end = findEnd(text, i);
            if (end > i) {
                words.add(text.substring(i, end + 1));
                i = end;
            }
        }
        return words;
    }

    @SuppressWarnings("unchecked")
    private boolean checkFrom(String text, int start) {
        Map<String, Map> current = dfaMap;
        for (int i = start; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            current = current.get(ch);
            if (current == null) {
                return false;
            }
            if (current.containsKey(END)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private int findEnd(String text, int start) {
        Map<String, Map> current = dfaMap;
        int matchEnd = -1;
        for (int i = start; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            current = current.get(ch);
            if (current == null) {
                break;
            }
            if (current.containsKey(END)) {
                matchEnd = i;
            }
        }
        return matchEnd;
    }
}
