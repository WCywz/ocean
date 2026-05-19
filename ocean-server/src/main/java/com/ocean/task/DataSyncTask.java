package com.ocean.task;

import com.ocean.service.ForecastService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据同步定时任务：每天运行模型预报，结果写入 forecast_grid。
 * <p>
 * 启用方式：在 OceanApplication 类上添加 {@code @EnableScheduling} 注解。
 */
@Slf4j
@Component
public class DataSyncTask {

    @Autowired
    private ForecastService forecastService;

    /** 每天凌晨 2 点执行（系统日期推进后再跑预报） */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncData() {
        log.info(">>>>>> 定时预报任务开始");
        try {
            Map<String, Object> result = forecastService.runForecast();
            log.info("<<<<<< 定时预报任务完成: {}", result);
        } catch (Exception e) {
            log.error("<<<<<< 定时预报任务失败", e);
        }
    }
}
