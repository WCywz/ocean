package com.ocean.task;

import com.ocean.service.ForecastService;
import com.ocean.service.ObservationIngestService;
import com.ocean.service.PipelineLockService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class SystemDateTask {

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private ObservationIngestService observationIngestService;

    @Autowired
    private ForecastService forecastService;

    @Autowired
    private HealthAssessmentTask healthAssessmentTask;

    @Autowired
    private PipelineLockService pipelineLock;

    /** 每天凌晨 0:05 执行，系统日期 +1 天，串联触发观测入库、预报和健康评估 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void advanceSystemDate() {
        if (!pipelineLock.tryLock()) {
            log.warn("流水线正在执行中，跳过主链路");
            return;
        }
        try {
            systemConfigService.advanceDay();
            LocalDate today = systemConfigService.getSystemDate();
            log.info("系统日期已推进至: {}", today);
            runPipeline(today);
        } finally {
            pipelineLock.unlock();
        }
    }

    private void runPipeline(LocalDate today) {
        try {
            observationIngestService.ingestDate(today);
            log.info("观测数据入库完成: {}", today);
        } catch (Exception e) {
            log.error("观测数据入库失败: {}", today, e);
        }

        try {
            forecastService.runForecast();
            log.info("模型预报完成: {}", today);
        } catch (Exception e) {
            log.error("模型预报失败: {}", today, e);
        }

        try {
            healthAssessmentTask.run(today);
            log.info("健康评估完成: {}", today);
        } catch (Exception e) {
            log.error("健康评估失败: {}", today, e);
        }
    }
}
