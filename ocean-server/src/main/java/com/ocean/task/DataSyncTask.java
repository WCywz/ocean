package com.ocean.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据同步定时任务
 * <p>
 * 启用方式：在 OceanApplication 类上添加 {@code @EnableScheduling} 注解。
 */
@Slf4j
@Component
public class DataSyncTask {

    /** 每 24 小时执行一次 */
    @Scheduled(fixedRate = 24 * 60 * 60 * 1000)
    public void syncData() {
        log.info("定时任务就绪，当前时间: {}", System.currentTimeMillis());
    }
}
