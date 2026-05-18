package com.ocean.task;

import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SystemDateTask {

    @Autowired
    private SystemConfigService systemConfigService;

    /** 每天凌晨 0:05 执行，系统日期 +1 天 */
    @Scheduled(cron = "0 5 0 * * ?")
    public void advanceSystemDate() {
        systemConfigService.advanceDay();
        log.info("系统日期已推进至: {}", systemConfigService.getSystemDate());
    }
}
