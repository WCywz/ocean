package com.ocean.task;

import com.ocean.service.ForecastService;
import com.ocean.service.ObservationIngestService;
import com.ocean.service.PipelineLockService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class StartupCatchUpTask {

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

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void catchUp() {
        try {
            doCatchUp();
        } catch (Exception e) {
            log.error("启动补录顶层异常", e);
        }
    }

    private void doCatchUp() {
        LocalDate today = systemConfigService.getSystemDate();
        log.info("启动补录检查，系统日期: {}", today);

        if (!pipelineLock.tryLock()) {
            log.info("流水线正在执行中，启动补录跳过: {}", today);
            return;
        }
        try {
            boolean ingested = pipelineLock.isObservationIngested(today);
            boolean forecasted = pipelineLock.isForecastComplete(today);
            boolean assessed = pipelineLock.isHealthAssessed(today);

            if (ingested && forecasted && assessed) {
                log.info("数据流水线完整，无需补录: {}", today);
                return;
            }

            log.info("数据缺失 — 观测:{} 预报:{} 健康:{}，开始补录: {}",
                    !ingested, !forecasted, !assessed, today);

            if (!ingested) {
                try {
                    observationIngestService.ingestDate(today);
                    log.info("补录观测数据完成: {}", today);
                } catch (Exception e) {
                    log.error("补录观测数据失败: {}", today, e);
                }
            }

            // 老管线预报已由 Quartz 调度接管
            // if (!forecasted) {
            //     try {
            //         forecastService.runForecast();
            //         log.info("补录模型预报完成: {}", today);
            //     } catch (Exception e) {
            //     log.error("补录模型预报失败: {}", today, e);
            //     }
            // }

            if (!assessed) {
                try {
                    healthAssessmentTask.run(today);
                    log.info("补录健康评估完成: {}", today);
                } catch (Exception e) {
                    log.error("补录健康评估失败: {}", today, e);
                }
            }
        } finally {
            pipelineLock.unlock();
        }

        log.info("启动补录完成: {}", today);
    }
}
