package com.site.blog.my.core.config;

import com.site.blog.my.core.service.StaticRenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务配置
 */
@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    @Autowired
    private StaticRenderService staticRenderService;

    /**
     * 每天凌晨 2 点执行全量渲染
     * cron 表达式：秒 分 时 日 月 周
     * 0 0 2 * * ? = 每天凌晨 2:00:00
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void renderAllBlogsDaily() {
        logger.info("========================================");
        logger.info("  定时任务：开始每日全量渲染");
        logger.info("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            StaticRenderService.RenderResult result = staticRenderService.renderAllBlogs();
            
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            
            if (result.isSuccess()) {
                logger.info("每日渲染完成，耗时 {}s", elapsed);
                logger.info("结果: {}", result.getMessage());
            } else {
                logger.error("每日渲染失败: {}", result.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("每日渲染异常", e);
        }
        
        logger.info("========================================");
    }

    /**
     * 容器启动后延迟 5 分钟执行首次渲染
     * 确保应用完全启动后再开始渲染
     */
    @Scheduled(initialDelay = 5 * 60 * 1000, fixedRate = Long.MAX_VALUE)
    public void renderOnStartup() {
        logger.info("========================================");
        logger.info("  容器启动：执行首次渲染");
        logger.info("========================================");
        
        long startTime = System.currentTimeMillis();
        
        try {
            StaticRenderService.RenderResult result = staticRenderService.renderAllBlogs();
            
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            
            if (result.isSuccess()) {
                logger.info("首次渲染完成，耗时 {}s", elapsed);
                logger.info("结果: {}", result.getMessage());
            } else {
                logger.error("首次渲染失败: {}", result.getMessage());
            }
            
        } catch (Exception e) {
            logger.error("首次渲染异常", e);
        }
        
        logger.info("========================================");
    }
}
