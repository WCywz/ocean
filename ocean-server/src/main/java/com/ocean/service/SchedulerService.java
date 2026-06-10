package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.Model;
import com.ocean.entity.ModelSchedule;
import com.ocean.entity.ModelVersion;
import com.ocean.mapper.ModelMapper;
import com.ocean.mapper.ModelScheduleMapper;
import com.ocean.mapper.ModelVersionMapper;
import com.ocean.task.ModelForecastJob;
import com.ocean.util.CronUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SchedulerService implements ApplicationRunner {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private ModelVersionMapper versionMapper;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private ModelScheduleMapper scheduleMapper;

    private static final String JOB_GROUP = "model-forecast";
    private static final String TRIGGER_GROUP = "model-forecast";

    /** 注册一个版本的所有活跃调度到调度器 */
    public void schedule(Long versionId) {
        ModelVersion mv = versionMapper.selectById(versionId);
        if (mv == null) return;
        Model model = modelMapper.selectById(mv.getModelId());

        List<ModelSchedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<ModelSchedule>()
                        .eq(ModelSchedule::getVersionId, versionId)
                        .eq(ModelSchedule::getIsActive, 1));

        if (schedules.isEmpty()) {
            log.warn("版本 {} 无活跃调度，跳过", mv.getVersionLabel());
            return;
        }

        String jobName = jobName(versionId);

        JobDetail job = JobBuilder.newJob(ModelForecastJob.class)
                .withIdentity(jobName, JOB_GROUP)
                .usingJobData("versionId", versionId)
                .usingJobData("modelId", mv.getModelId())
                .usingJobData("modelName", model != null ? model.getModelName() : "")
                .usingJobData("versionLabel", mv.getVersionLabel())
                .usingJobData("modelType", model != null ? model.getModelType() : "")
                .storeDurably()
                .build();

        try {
            if (scheduler.checkExists(job.getKey())) {
                scheduler.deleteJob(job.getKey());
            }
            scheduler.addJob(job, true);

            int count = 0;
            for (ModelSchedule s : schedules) {
                String cron = CronUtil.toCron(s.getRepetition(), s.getDayOfWeek(), s.getScheduleTime(), s.getScheduleDate());
                CronTrigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(triggerName(s.getId()), TRIGGER_GROUP)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                        .forJob(job)
                        .usingJobData("scheduleId", s.getId())
                        .build();
                scheduler.scheduleJob(trigger);
                count++;
            }
            log.info("调度器: 已注册 {} {} ({}个调度)", model != null ? model.getModelName() : "", mv.getVersionLabel(), count);
        } catch (SchedulerException e) {
            log.error("调度器: 注册失败 versionId={}", versionId, e);
        }
    }

    /** 从调度器移除一个版本 */
    public void unschedule(Long versionId) {
        try {
            JobKey jobKey = new JobKey(jobName(versionId), JOB_GROUP);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("调度器: 已移除 versionId={}", versionId);
            }
        } catch (SchedulerException e) {
            log.error("调度器: 移除失败 versionId={}", versionId, e);
        }
    }

    /** 更新一个版本的 cron（先删后建） */
    public void reschedule(Long versionId) {
        unschedule(versionId);
        ModelVersion mv = versionMapper.selectById(versionId);
        if (mv != null && "RUNNING".equals(mv.getStatus())) {
            schedule(versionId);
        }
    }

    /** 激活单条调度 */
    public void scheduleOne(Long scheduleId) {
        ModelSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null || s.getIsActive() != 1) return;

        ModelVersion mv = versionMapper.selectById(s.getVersionId());
        if (mv == null || !"RUNNING".equals(mv.getStatus())) return;

        String cron = CronUtil.toCron(s.getRepetition(), s.getDayOfWeek(), s.getScheduleTime(), s.getScheduleDate());
        String jobName = jobName(s.getVersionId());

        try {
            if (!scheduler.checkExists(new JobKey(jobName, JOB_GROUP))) {
                schedule(s.getVersionId());
                return;
            }

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerName(s.getId()), TRIGGER_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .forJob(jobName, JOB_GROUP)
                    .usingJobData("scheduleId", s.getId())
                    .build();

            if (scheduler.checkExists(trigger.getKey())) {
                scheduler.rescheduleJob(trigger.getKey(), trigger);
            } else {
                scheduler.scheduleJob(trigger);
            }
            log.info("调度器: 单条调度已注册 scheduleId={}", scheduleId);
        } catch (SchedulerException e) {
            log.error("调度器: 单条调度注册失败 scheduleId={}", scheduleId, e);
        }
    }

    /** 移除单条调度 */
    public void unscheduleOne(Long scheduleId) {
        try {
            TriggerKey tk = new TriggerKey(triggerName(scheduleId), TRIGGER_GROUP);
            if (scheduler.checkExists(tk)) {
                scheduler.unscheduleJob(tk);
                log.info("调度器: 单条调度已移除 scheduleId={}", scheduleId);
            }
        } catch (SchedulerException e) {
            log.error("调度器: 单条调度移除失败 scheduleId={}", scheduleId, e);
        }
    }

    /** 更新单条调度的 Cron */
    public void rescheduleOne(Long scheduleId) {
        unscheduleOne(scheduleId);
        ModelSchedule s = scheduleMapper.selectById(scheduleId);
        if (s != null && s.getIsActive() == 1) {
            scheduleOne(scheduleId);
        }
    }

    /** 启动时扫描所有 RUNNING 版本，重新注册到调度器 */
    @Override
    public void run(ApplicationArguments args) {
        List<ModelVersion> runningList = versionMapper.selectList(
                new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getStatus, "RUNNING"));
        log.info("调度器启动: 扫描到 {} 个运行中的版本", runningList.size());
        for (ModelVersion mv : runningList) {
            schedule(mv.getId());
        }
    }

    private String jobName(Long versionId) {
        return "forecast-v" + versionId;
    }

    private String triggerName(Long scheduleId) {
        return "trigger-s" + scheduleId;
    }
}
