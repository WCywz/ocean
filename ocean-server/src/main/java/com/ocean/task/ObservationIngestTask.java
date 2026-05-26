package com.ocean.task;

import com.ocean.service.ObservationIngestService;
import com.ocean.service.PipelineLockService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 每日观测数据入库（01:00）。
 * 主链路已由 SystemDateTask 在 00:05 执行，此处作为兜底补录。
 */
@Slf4j
@Component
public class ObservationIngestTask {

    @Autowired
    private ObservationIngestService observationIngestService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Autowired
    private PipelineLockService pipelineLock;

    @Scheduled(cron = "0 0 1 * * ?")
    public void ingestDaily() {
        LocalDate today = systemConfigService.getSystemDate();
        log.info(">>>>>> 观测数据入库兜底检查: {}", today);

        if (pipelineLock.isObservationIngested(today)) {
            log.info("观测数据已存在，跳过: {}", today);
            return;
        }
        if (!pipelineLock.tryLock()) {
            log.info("流水线正在执行中，跳过: {}", today);
            return;
        }
        try {
            observationIngestService.ingestDate(today);
            log.info("<<<<<< 观测数据入库完成: {}", today);
        } catch (Exception e) {
            log.error("<<<<<< 观测数据入库失败: {}", today, e);
        } finally {
            pipelineLock.unlock();
        }
    }
}
