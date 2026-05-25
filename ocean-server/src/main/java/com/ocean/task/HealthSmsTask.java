package com.ocean.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.SysUser;
import com.ocean.mapper.SysUserMapper;
import com.ocean.service.HealthService;
import com.ocean.sms.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HealthSmsTask {

    @Autowired private HealthService healthService;
    @Autowired private SmsService smsService;
    @Autowired private SysUserMapper sysUserMapper;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailySms() {
        log.info(">>>>>> 健康短信任务开始");

        try {
            String content = healthService.buildDailySummary();
            if (content == null) {
                log.info("无健康数据，跳过短信发送");
                return;
            }

            var admins = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getRole, "ADMIN")
                            .eq(SysUser::getStatus, 1));

            if (admins.isEmpty()) {
                log.warn("无活跃管理员，跳过推送");
                return;
            }

            for (SysUser admin : admins) {
                try {
                    boolean ok = smsService.send(admin.getPhone(), content);
                    if (ok) {
                        log.info("健康日报已推送至 {} (微信)", admin.getUsername());
                    } else {
                        log.error("健康日报推送失败 {}", admin.getUsername());
                    }
                } catch (Exception e) {
                    log.error("健康日报推送异常 {}", admin.getUsername(), e);
                }
            }

            log.info("<<<<<< 健康短信任务完成，推送 {} 条", admins.size());
        } catch (Exception e) {
            log.error("<<<<<< 健康短信任务失败", e);
        }
    }
}
