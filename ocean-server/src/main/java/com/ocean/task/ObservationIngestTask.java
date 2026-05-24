package com.ocean.task;

import com.ocean.service.ObservationIngestService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Daily observation data ingestion: ingests observation_data and
 * observation_grid for the current system date (already advanced by SystemDateTask).
 * Runs at 01:00, after SystemDateTask (00:05) but before DataSyncTask (02:00).
 */
@Slf4j
@Component
public class ObservationIngestTask {

    @Autowired
    private ObservationIngestService observationIngestService;

    @Autowired
    private SystemConfigService systemConfigService;

    @Scheduled(cron = "0 0 1 * * ?")
    public void ingestDaily() {
        log.info(">>>>>> 每日观测数据入库开始");
        try {
            observationIngestService.ingestDate(systemConfigService.getSystemDate());
            log.info("<<<<<< 每日观测数据入库完成");
        } catch (Exception e) {
            log.error("<<<<<< 每日观测数据入库失败", e);
        }
    }
}
