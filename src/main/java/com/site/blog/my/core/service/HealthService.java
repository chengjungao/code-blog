package com.site.blog.my.core.service;

import org.springframework.web.multipart.MultipartFile;

public interface HealthService {

    /**
     * 拍照提取配料表文字（OCR）
     */
    String analyzeIngredientsOcr(MultipartFile image);

    /**
     * 配料表文本 AI 分析
     */
    String analyzeIngredientsV3(String content);

    /**
     * AI 查询单个配料知识
     */
    String lookupIngredient(String ingredientName);
}
