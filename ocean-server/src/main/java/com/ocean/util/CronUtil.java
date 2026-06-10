package com.ocean.util;

import java.time.LocalDate;  // kept for API compatibility of toCron signature
import java.time.LocalTime;

/**
 * Cron 表达式与调度配置互转工具
 */
public class CronUtil {

    /**
     * 根据调度配置生成 Cron 表达式。
     * <p>
     * 重要：所有调度统一使用纯时间 Cron（不带日期），由 Job 执行时根据当前
     * system_date 动态判断是否真正执行。原因：系统使用模拟日期（system_date），
     * Quartz 只认真实时间——如果 ONCE 调度把系统日期编码进 cron，Quartz 会以
     * 真实日期解释，导致过去日期 misfire 或未来日期无限等待。
     * </p>
     */
    public static String toCron(String repetition, Integer dayOfWeek, LocalTime time, LocalDate scheduleDate) {
        int hour = time.getHour();
        int minute = time.getMinute();
        // All schedules fire daily at the configured time.
        // The Job checks system_date vs schedule config to decide whether to run.
        return String.format("0 %d %d * * ?", minute, hour);
    }

    /**
     * 从 Cron 表达式反向解析出调度信息。
     * <p>
     * 自 system_date 守卫方案后，所有调度统一使用纯时间 Cron（0 MM HH * * ?），
     * 不再编码 repetition/dayOfWeek/scheduleDate 到 Cron 中。
     * 此方法仅用于从 Cron 提取时间信息（HH:MM），repetition 始终返回 DAILY。
     * </p>
     */
    public static CronInfo fromCron(String cron) {
        if (cron == null || cron.isEmpty()) return null;
        String[] parts = cron.trim().split("\\s+");
        if (parts.length < 6) return null;
        try {
            int minute = Integer.parseInt(parts[1]);
            int hour = Integer.parseInt(parts[2]);
            CronInfo info = new CronInfo();
            info.time = LocalTime.of(hour, minute);
            info.repetition = "DAILY";
            return info;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class CronInfo {
        public String repetition;
        public Integer dayOfWeek;
        public LocalTime time;
    }
}
