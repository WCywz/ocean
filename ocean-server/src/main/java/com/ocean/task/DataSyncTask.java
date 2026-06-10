package com.ocean.task;

import com.ocean.service.ForecastService;
import com.ocean.service.PipelineLockService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * 每日模型预报（02:00）。
 * 主链路已由 SystemDateTask 在 00:05 执行，此处作为兜底补录。
 */
@Slf4j
@Component
public class DataSyncTask {

    @Autowired
    private ForecastService forecastService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private PipelineLockService pipelineLock;

    @Scheduled(cron = "0 0 2 * * ?")
    public void syncData() {
        LocalDate today = systemConfigService.getSystemDate();
        log.info(">>>>>> 模型预报兜底检查: {}", today);

        if (pipelineLock.isForecastComplete(today)) {
            log.info("预报数据已存在，跳过: {}", today);
            return;
        }
        if (!pipelineLock.tryLock()) {
            log.info("流水线正在执行中，跳过: {}", today);
            return;
        }
        // 老管线预报已由 Quartz 调度接管
        // try {
        //     Map<String, Object> result = forecastService.runForecast();
        //     log.info("<<<<<< 模型预报完成: {}", result);
        // } catch (Exception e) {
        //     log.error("<<<<<< 模型预报失败", e);
        // } finally {
        //     pipelineLock.unlock();
        // }
        pipelineLock.unlock();
    }
}
