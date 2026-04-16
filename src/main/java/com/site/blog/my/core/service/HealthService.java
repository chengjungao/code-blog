package com.site.blog.my.core.service;

import org.springframework.web.multipart.MultipartFile;

public interface HealthService {

    /**
     * 分析图片中的成分
     * @param image 图片文件
     * @return 分析结果
     */
    @Deprecated
    String analyzeIngredients(MultipartFile image);

    /**
     * 分析图片中的成分 V2
     * @param image 图片文件
     * @return 分析结果
     */
    String analyzeIngredientsV2(MultipartFile image);

    /**
     * 估算图片中的食物热量
     * @param image 图片文件
     * @return 热量估算结果
     */
    String estimateCalories(MultipartFile image);

    /**
     * 提取配料表
     * @param image 图片文件
     * @return 分析结果
     */
    String analyzeIngredientsOcr(MultipartFile image);

    /**
     * 分析餐食内容
     * @param image 图片文件
     * @return 热量估算结果
     */
    String estimateCaloriesOcr(MultipartFile image);


    /**
     * 分析成分
     * @param content
     * @return
     */
    String analyzeIngredientsV3(String content);

    /**
     * 估算热量
     * @param content
     * @return
     */
    String estimateCaloriesV3(String content);



}
