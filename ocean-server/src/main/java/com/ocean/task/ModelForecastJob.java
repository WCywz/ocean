package com.ocean.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.AlertEvent;
import com.ocean.entity.ModelRunLog;
import com.ocean.entity.ModelSchedule;
import com.ocean.mapper.AlertEventMapper;
import com.ocean.mapper.ModelRunLogMapper;
import com.ocean.mapper.ModelScheduleMapper;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@DisallowConcurrentExecution
public class ModelForecastJob implements Job {

    @Autowired
    private ModelRunLogMapper runLogMapper;

    @Autowired
    private AlertEventMapper alertEventMapper;

    @Autowired
    private ModelScheduleMapper scheduleMapper;

    @Autowired
    private com.ocean.service.ForecastService forecastService;

    @Autowired
    private com.ocean.service.SystemConfigService systemConfigService;

    @Override
    public void execute(JobExecutionContext context) {
        Long versionId = context.getJobDetail().getJobDataMap().getLong("versionId");
        Long modelId = context.getJobDetail().getJobDataMap().getLong("modelId");
        String modelName = context.getJobDetail().getJobDataMap().getString("modelName");
        String versionLabel = context.getJobDetail().getJobDataMap().getString("versionLabel");
        String modelType = context.getJobDetail().getJobDataMap().getString("modelType");

        // 系统日期守卫：所有调度用纯时间 cron（每天触发），由 Job 根据
        // system_date 判断是否真正执行。避免 Quartz 真实时间与模拟时间脱节。
        Long scheduleId = null;
        Object sid = context.getMergedJobDataMap().get("scheduleId");
        if (sid != null) {
            scheduleId = ((Number) sid).longValue();
            ModelSchedule schedule = scheduleMapper.selectById(scheduleId);
            if (schedule != null && !shouldRunNow(schedule)) {
                log.info("调度跳过: {}/{} system_date={} 不匹配 (规则={}, 期望日期={})",
                        modelName, versionLabel, systemConfigService.getSystemDate(),
                        schedule.getRepetition(), schedule.getScheduleDate());
                return;
            }
        }

        log.info("开始执行模型预测: {} {}, versionId={}, modelType={}", modelName, versionLabel, versionId, modelType);

        ModelRunLog runLog = new ModelRunLog();
        runLog.setVersionId(versionId);
        runLog.setModelId(modelId);
        runLog.setModelName(modelName);
        runLog.setVersionLabel(versionLabel);
        runLog.setStartTime(LocalDateTime.now());
        runLog.setStatus("RUNNING");
        runLogMapper.insert(runLog);

        try {
            Map<String, Object> forecastResult = forecastService.runForecast(versionId, modelId, modelType);

            runLog.setEndTime(LocalDateTime.now());
            runLog.setDurationMs(java.time.Duration.between(runLog.getStartTime(), runLog.getEndTime()).toMillis());
            boolean success = Boolean.TRUE.equals(forecastResult.get("success"));
            runLog.setStatus(success ? "SUCCESS" : "FAILED");
            if (!success) {
                runLog.setErrorMessage(String.valueOf(forecastResult.getOrDefault("message", "未知错误")));
            }
            runLog.setOutputSummary("模型预测完成: " + modelName + " " + versionLabel
                    + (forecastResult.get("count") != null ? " (产出 " + forecastResult.get("count") + " 条)" : ""));
            runLogMapper.updateById(runLog);
            log.info("模型预测完成: {} {}, 耗时{}ms, 产出{}条", modelName, versionLabel,
                    runLog.getDurationMs(), forecastResult.get("count"));

            if (!success) {
                throw new RuntimeException(String.valueOf(forecastResult.getOrDefault("message", "预测失败")));
            }

            // ONCE 调度执行成功后自动停用（复用守卫中的 scheduleId）
            if (scheduleId != null) {
                ModelSchedule s = scheduleMapper.selectById(scheduleId);
                if (s != null && "ONCE".equals(s.getRepetition()) && s.getIsActive() == 1) {
                    s.setIsActive(0);
                    scheduleMapper.updateById(s);
                    log.info("ONCE 调度已自动停用: scheduleId={}", scheduleId);
                }
            }
        } catch (Exception e) {
            log.error("模型预测失败: {} {}", modelName, versionLabel, e);
            runLog.setEndTime(LocalDateTime.now());
            runLog.setDurationMs(java.time.Duration.between(runLog.getStartTime(), runLog.getEndTime()).toMillis());
            runLog.setStatus("FAILED");
            runLog.setErrorMessage(e.getMessage());
            runLogMapper.updateById(runLog);

            // 创建执行失败告警
            AlertEvent alert = new AlertEvent();
            alert.setVersionId(versionId);
            alert.setModelId(modelId);
            alert.setModelName(modelName);
            alert.setVersionLabel(versionLabel);
            alert.setAlertType("EXECUTION_FAILED");
            alert.setMessage(modelName + " " + versionLabel + " 执行失败: " + e.getMessage());
            alert.setRunLogId(runLog.getId());
            alertEventMapper.insert(alert);

            // 检查连续失败次数
            checkConsecutiveFailures(versionId, modelId, modelName, versionLabel);
        }
    }

    /**
     * 判断调度是否应在当前系统日期执行。
     * <p>
     * 所有调度统一使用纯时间 cron（每天触发），由本方法根据 system_date
     * 和调度配置决定是否真正执行。这样避免了 Quartz 真实时间与模拟系统时间
     * 的脱节问题（如 ONCE 调度编码了模拟日期到 cron，Quartz 用真实日期解释）。
     * </p>
     */
    private boolean shouldRunNow(ModelSchedule schedule) {
        java.time.LocalDate sysDate = systemConfigService.getSystemDate();
        String rep = schedule.getRepetition();
        if ("DAILY".equals(rep)) {
            return true;
        }
        if ("WEEKLY".equals(rep)) {
            int sysDow = sysDate.getDayOfWeek().getValue(); // 1=Mon..7=Sun
            int schedDow = schedule.getDayOfWeek() != null ? schedule.getDayOfWeek() : 1;
            return sysDow == schedDow;
        }
        if ("ONCE".equals(rep)) {
            java.time.LocalDate schedDate = schedule.getScheduleDate();
            return schedDate != null && sysDate.equals(schedDate);
        }
        return true;
    }

    private void checkConsecutiveFailures(Long versionId, Long modelId, String modelName, String versionLabel) {
        List<ModelRunLog> recentLogs = runLogMapper.selectList(
                new LambdaQueryWrapper<ModelRunLog>()
                        .eq(ModelRunLog::getVersionId, versionId)
                        .orderByDesc(ModelRunLog::getStartTime)
                        .last("LIMIT 3"));

        boolean allFailed = recentLogs.size() >= 3 && recentLogs.stream().allMatch(
                log -> "FAILED".equals(log.getStatus()));

        if (allFailed) {
            Long exists = alertEventMapper.selectCount(
                    new LambdaQueryWrapper<AlertEvent>()
                            .eq(AlertEvent::getVersionId, versionId)
                            .eq(AlertEvent::getAlertType, "CONSECUTIVE_FAILURES")
                            .gt(AlertEvent::getCreateTime, LocalDateTime.now().minusHours(6)));
            if (exists == 0) {
                AlertEvent alert = new AlertEvent();
                alert.setVersionId(versionId);
                alert.setModelId(modelId);
                alert.setModelName(modelName);
                alert.setVersionLabel(versionLabel);
                alert.setAlertType("CONSECUTIVE_FAILURES");
                alert.setMessage(modelName + " " + versionLabel + " 已连续失败 " + recentLogs.size() + " 次，需要人工排查");
                alertEventMapper.insert(alert);
                log.warn("连续失败告警: {} {} 已连续失败{}次", modelName, versionLabel, recentLogs.size());
            }
        }
    }
}
