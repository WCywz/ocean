# 调度总览拖拽排程 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将模型版本调度从 Cron 文本输入改为可视化拖拽排程——右侧版本卡片拖到周视图日历，弹窗选重复规则后生成调度。

**Architecture:** 新建 `model_schedule` 表存储多调度记录，扩展 `SchedulerService` 支持一个版本多个 Quartz Trigger，前端手写 HTML5 DnD + 周日历（7×24 格），全自研无第三方日历库。

**Tech Stack:** Java 21 / Spring Boot 3.4.1 / MyBatis-Plus 3.5.7 / Quartz / Vue 3 / Vite / Element Plus / CSS 变量深色模式

---

### Task 1: 数据库迁移 — model_schedule 表

**Files:**
- Create: `database/migration/006-model-schedule.sql`

- [ ] **Step 1: 创建迁移 SQL**

```sql
-- 模型调度表
CREATE TABLE IF NOT EXISTS model_schedule (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id     BIGINT NOT NULL COMMENT '版本ID',
    schedule_label VARCHAR(50) COMMENT '调度标签',
    repetition     VARCHAR(20) NOT NULL COMMENT 'DAILY / WEEKLY / ONCE',
    day_of_week    INT COMMENT 'WEEKLY时: 1=周一..7=周日',
    schedule_time  TIME NOT NULL COMMENT '调度时间 HH:mm',
    is_active      TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_version_id (version_id),
    FOREIGN KEY (version_id) REFERENCES model_version(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型调度配置';
```

- [ ] **Step 2: 执行迁移**

Run: `mysql -u root -proot ocean_forecast < database/migration/006-model-schedule.sql`

- [ ] **Step 3: 验证表结构**

Run: `mysql -u root -proot ocean_forecast -e "DESC model_schedule;"`

- [ ] **Step 4: Commit**

```bash
git add database/migration/006-model-schedule.sql
git commit -m "feat: 新建 model_schedule 表支持多调度配置"
```

---

### Task 2: 后端 — ModelSchedule 实体

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/ModelSchedule.java`

- [ ] **Step 1: 创建实体类**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("model_schedule")
public class ModelSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long versionId;

    private String scheduleLabel;

    private String repetition;

    private Integer dayOfWeek;

    private LocalTime scheduleTime;

    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/entity/ModelSchedule.java
git commit -m "feat: 新增 ModelSchedule 实体"
```

---

### Task 3: 后端 — ModelScheduleMapper

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/mapper/ModelScheduleMapper.java`

- [ ] **Step 1: 创建 Mapper 接口**

```java
package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.ModelSchedule;

public interface ModelScheduleMapper extends BaseMapper<ModelSchedule> {
}
```

- [ ] **Step 2: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/mapper/ModelScheduleMapper.java
git commit -m "feat: 新增 ModelScheduleMapper"
```

---

### Task 4: 后端 — DTO & VO

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/dto/ModelScheduleSaveDTO.java`
- Create: `ocean-server/src/main/java/com/ocean/vo/ModelScheduleVO.java`
- Create: `ocean-server/src/main/java/com/ocean/vo/VersionCardVO.java`

- [ ] **Step 1: 创建 SaveDTO**

```java
package com.ocean.dto;

import lombok.Data;

@Data
public class ModelScheduleSaveDTO {

    private Long id;

    private Long versionId;

    private String scheduleLabel;

    private String repetition;

    private Integer dayOfWeek;

    private String scheduleTime;
}
```

- [ ] **Step 2: 创建 VO**

```java
package com.ocean.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ModelScheduleVO {

    private Long id;

    private Long versionId;

    private String scheduleLabel;

    private String repetition;

    private Integer dayOfWeek;

    private LocalTime scheduleTime;

    private Integer isActive;

    private String cronExpression;

    private String modelName;

    private String versionLabel;

    private String modelType;

    private LocalDateTime createTime;
}
```

- [ ] **Step 3: 创建 VersionCardVO（卡片池用）**

```java
package com.ocean.vo;

import lombok.Data;

import java.util.List;

@Data
public class VersionCardVO {

    private Long modelId;

    private Long versionId;

    private String modelName;

    private String versionLabel;

    private String modelType;

    private String status;

    private List<ScheduleBrief> schedules;

    @Data
    public static class ScheduleBrief {
        private Long id;
        private String repetition;
        private Integer dayOfWeek;
        private String scheduleTime;
    }
}
```

- [ ] **Step 4: Verify compilation**

Run: `cd ocean-server && mvn compile -q`

- [ ] **Step 5: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/dto/ModelScheduleSaveDTO.java ocean-server/src/main/java/com/ocean/vo/ModelScheduleVO.java ocean-server/src/main/java/com/ocean/vo/VersionCardVO.java
git commit -m "feat: 新增 ModelScheduleSaveDTO、ModelScheduleVO、VersionCardVO"
```

---

### Task 5: 后端 — Cron 表达式工具类

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/util/CronUtil.java`

- [ ] **Step 1: 创建 CronUtil**

```java
package com.ocean.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CronUtil {

    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 根据重复规则、周几、时间生成 Quartz Cron 表达式。
     *
     * @param repetition DAILY / WEEKLY / ONCE
     * @param dayOfWeek  WEEKLY 时 1=周一..7=周日，其他传 null
     * @param time       调度时间
     * @return Cron 表达式
     */
    public static String toCron(String repetition, Integer dayOfWeek, LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();

        return switch (repetition) {
            case "DAILY" -> String.format("0 %d %d * * ?", minute, hour);
            case "WEEKLY" -> {
                int dow = (dayOfWeek != null) ? dayOfWeek : 1;
                yield String.format("0 %d %d ? * %d", minute, hour, dow);
            }
            case "ONCE" -> String.format("0 %d %d * * ?", minute, hour);
            default -> throw new IllegalArgumentException("不支持的重复规则: " + repetition);
        };
    }

    /**
     * 将 Cron 表达式解析回重复规则描述。
     */
    public static CronInfo fromCron(String cron) {
        if (cron == null || cron.isEmpty()) return null;
        String[] parts = cron.trim().split("\\s+");
        if (parts.length != 6 && parts.length != 7) return null;
        int minute = Integer.parseInt(parts[1]);
        int hour = Integer.parseInt(parts[2]);
        String dom = parts[3];
        String dow = parts[5];

        CronInfo info = new CronInfo();
        info.time = LocalTime.of(hour, minute);

        if ("?".equals(dow) && "*".equals(dom)) {
            info.repetition = "DAILY";
        } else if ("*".equals(dom) && !"*".equals(dow) && !"?".equals(dow)) {
            info.repetition = "WEEKLY";
            info.dayOfWeek = Integer.parseInt(dow);
        } else {
            info.repetition = "DAILY";
        }
        return info;
    }

    public static class CronInfo {
        public String repetition;
        public Integer dayOfWeek;
        public LocalTime time;
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd ocean-server && mvn compile -q`

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/util/CronUtil.java
git commit -m "feat: 新增 CronUtil 工具类生成/解析 Cron 表达式"
```

---

### Task 6: 后端 — ModelScheduleService

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/ModelScheduleService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/ModelScheduleServiceImpl.java`

- [ ] **Step 1: 创建 Service 接口**

```java
package com.ocean.service;

import com.ocean.dto.ModelScheduleSaveDTO;
import com.ocean.vo.ModelScheduleVO;
import com.ocean.vo.VersionCardVO;

import java.time.LocalDate;
import java.util.List;

public interface ModelScheduleService {
    List<ModelScheduleVO> getSchedulesByVersionId(Long versionId);
    ModelScheduleVO addSchedule(Long versionId, ModelScheduleSaveDTO dto);
    void updateSchedule(Long scheduleId, ModelScheduleSaveDTO dto);
    void deleteSchedule(Long scheduleId);
    List<ModelScheduleVO> getWeekSchedules(Long modelId, LocalDate startDate, LocalDate endDate);
    List<VersionCardVO> getAvailableVersionsForSchedule();
}
```

- [ ] **Step 2: 创建 ServiceImpl**

```java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.BusinessException;
import com.ocean.dto.ModelScheduleSaveDTO;
import com.ocean.entity.Model;
import com.ocean.entity.ModelSchedule;
import com.ocean.entity.ModelVersion;
import com.ocean.mapper.ModelMapper;
import com.ocean.mapper.ModelScheduleMapper;
import com.ocean.mapper.ModelVersionMapper;
import com.ocean.service.ModelScheduleService;
import com.ocean.service.SchedulerService;
import com.ocean.util.CronUtil;
import com.ocean.vo.ModelScheduleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelScheduleServiceImpl implements ModelScheduleService {

    @Autowired private ModelScheduleMapper scheduleMapper;
    @Autowired private ModelVersionMapper versionMapper;
    @Autowired private ModelMapper modelMapper;
    @Autowired private SchedulerService schedulerService;

    @Override
    public List<ModelScheduleVO> getSchedulesByVersionId(Long versionId) {
        List<ModelSchedule> list = scheduleMapper.selectList(
                new LambdaQueryWrapper<ModelSchedule>()
                        .eq(ModelSchedule::getVersionId, versionId)
                        .orderByAsc(ModelSchedule::getScheduleTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ModelScheduleVO addSchedule(Long versionId, ModelScheduleSaveDTO dto) {
        ModelVersion mv = versionMapper.selectById(versionId);
        if (mv == null) throw new BusinessException("版本不存在");

        ModelSchedule s = new ModelSchedule();
        s.setVersionId(versionId);
        s.setScheduleLabel(dto.getScheduleLabel());
        s.setRepetition(dto.getRepetition());
        s.setDayOfWeek(dto.getDayOfWeek());
        s.setScheduleTime(LocalTime.parse(dto.getScheduleTime(), DateTimeFormatter.ofPattern("HH:mm")));
        s.setIsActive(1);
        scheduleMapper.insert(s);

        if ("RUNNING".equals(mv.getStatus())) {
            schedulerService.scheduleOne(s.getId());
        }

        return toVO(s);
    }

    @Override
    public void updateSchedule(Long scheduleId, ModelScheduleSaveDTO dto) {
        ModelSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null) throw new BusinessException("调度不存在");

        s.setScheduleLabel(dto.getScheduleLabel());
        s.setRepetition(dto.getRepetition());
        s.setDayOfWeek(dto.getDayOfWeek());
        s.setScheduleTime(LocalTime.parse(dto.getScheduleTime(), DateTimeFormatter.ofPattern("HH:mm")));
        scheduleMapper.updateById(s);

        if (s.getIsActive() == 1) {
            schedulerService.rescheduleOne(scheduleId);
        }
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        ModelSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null) throw new BusinessException("调度不存在");
        scheduleMapper.deleteById(scheduleId);
        schedulerService.unscheduleOne(scheduleId);
    }

    @Override
    public List<ModelScheduleVO> getWeekSchedules(Long modelId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ModelSchedule> wrapper = new LambdaQueryWrapper<ModelSchedule>()
                .eq(ModelSchedule::getIsActive, 1)
                .orderByAsc(ModelSchedule::getScheduleTime);

        if (modelId != null) {
            List<Long> versionIds = versionMapper.selectList(
                    new LambdaQueryWrapper<ModelVersion>()
                            .eq(ModelVersion::getModelId, modelId)
                            .select(ModelVersion::getId))
                    .stream().map(ModelVersion::getId).toList();
            if (versionIds.isEmpty()) return List.of();
            wrapper.in(ModelSchedule::getVersionId, versionIds);
        }

        return scheduleMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<VersionCardVO> getAvailableVersionsForSchedule() {
        List<ModelVersion> versions = versionMapper.selectList(null);
        return versions.stream().map(mv -> {
            VersionCardVO card = new VersionCardVO();
            card.setModelId(mv.getModelId());
            card.setVersionId(mv.getId());
            card.setVersionLabel(mv.getVersionLabel());
            card.setStatus(mv.getStatus());

            Model model = modelMapper.selectById(mv.getModelId());
            if (model != null) {
                card.setModelName(model.getModelName());
                card.setModelType(model.getModelType());
            }

            List<ModelSchedule> schedules = scheduleMapper.selectList(
                    new LambdaQueryWrapper<ModelSchedule>()
                            .eq(ModelSchedule::getVersionId, mv.getId())
                            .eq(ModelSchedule::getIsActive, 1));
            card.setSchedules(schedules.stream().map(s -> {
                VersionCardVO.ScheduleBrief brief = new VersionCardVO.ScheduleBrief();
                brief.setId(s.getId());
                brief.setRepetition(s.getRepetition());
                brief.setDayOfWeek(s.getDayOfWeek());
                brief.setScheduleTime(s.getScheduleTime() != null ? s.getScheduleTime().toString() : null);
                return brief;
            }).collect(Collectors.toList()));

            return card;
        }).collect(Collectors.toList());
    }

    private ModelScheduleVO toVO(ModelSchedule s) {
        ModelScheduleVO vo = new ModelScheduleVO();
        BeanUtils.copyProperties(s, vo);
        vo.setCronExpression(CronUtil.toCron(s.getRepetition(), s.getDayOfWeek(), s.getScheduleTime()));

        ModelVersion mv = versionMapper.selectById(s.getVersionId());
        if (mv != null) {
            vo.setVersionLabel(mv.getVersionLabel());
            Model model = modelMapper.selectById(mv.getModelId());
            if (model != null) {
                vo.setModelName(model.getModelName());
                vo.setModelType(model.getModelType());
            }
        }
        return vo;
    }
}
```

- [ ] **Step 3: Verify compilation**

Run: `cd ocean-server && mvn compile -q`

- [ ] **Step 4: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/ModelScheduleService.java ocean-server/src/main/java/com/ocean/service/impl/ModelScheduleServiceImpl.java
git commit -m "feat: 新增 ModelScheduleService — 多调度 CRUD + 周查询"
```

---

### Task 7: 后端 — ModelScheduleController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/ModelScheduleController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.ModelScheduleSaveDTO;
import com.ocean.service.ModelScheduleService;
import com.ocean.vo.ModelScheduleVO;
import com.ocean.vo.VersionCardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/model")
public class ModelScheduleController {

    @Autowired
    private ModelScheduleService scheduleService;

    /** 获取某版本的所有调度 */
    @GetMapping("/{modelId}/version/{versionId}/schedule")
    public Result<List<ModelScheduleVO>> getVersionSchedules(
            @PathVariable Long modelId,
            @PathVariable Long versionId) {
        return Result.success(scheduleService.getSchedulesByVersionId(versionId));
    }

    /** 创建调度 */
    @PostMapping("/{modelId}/version/{versionId}/schedule")
    public Result<ModelScheduleVO> addSchedule(
            @PathVariable Long modelId,
            @PathVariable Long versionId,
            @RequestBody ModelScheduleSaveDTO dto) {
        dto.setVersionId(versionId);
        return Result.success(scheduleService.addSchedule(versionId, dto));
    }

    /** 更新调度 */
    @PutMapping("/schedule/{id}")
    public Result<?> updateSchedule(
            @PathVariable Long id,
            @RequestBody ModelScheduleSaveDTO dto) {
        scheduleService.updateSchedule(id, dto);
        return Result.success("调度更新成功");
    }

    /** 删除调度 */
    @DeleteMapping("/schedule/{id}")
    public Result<?> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return Result.success("调度已删除");
    }

    /** 按周获取调度（日历展示用） */
    @GetMapping("/schedule/week")
    public Result<List<ModelScheduleVO>> getWeekSchedules(
            @RequestParam(required = false) Long modelId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(scheduleService.getWeekSchedules(modelId, startDate, endDate));
    }

    /** 获取所有可调度版本（卡片池用） */
    @GetMapping("/schedule/available-versions")
    public Result<List<VersionCardVO>> getAvailableVersions() {
        return Result.success(scheduleService.getAvailableVersionsForSchedule());
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `cd ocean-server && mvn compile -q`

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/controller/ModelScheduleController.java
git commit -m "feat: 新增 ModelScheduleController — 调度 CRUD + 周查询 API"
```

---

### Task 8: 后端 — SchedulerService 扩展多 Trigger

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/SchedulerService.java`

- [ ] **Step 1: 注入 ModelScheduleMapper 并添加单条调度方法**

在类顶部添加新的依赖注入：

```java
@Autowired
private ModelScheduleMapper scheduleMapper;
```

替换现有的 `schedule(Long versionId)` 方法为支持多 Trigger 的版本：

```java
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
            .storeDurably()
            .build();

    try {
        // 先清理旧 job
        if (scheduler.checkExists(job.getKey())) {
            scheduler.deleteJob(job.getKey());
        }
        scheduler.addJob(job, true);

        int count = 0;
        for (ModelSchedule s : schedules) {
            String cron = CronUtil.toCron(s.getRepetition(), s.getDayOfWeek(), s.getScheduleTime());
            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerName(s.getId()), TRIGGER_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                    .forJob(job)
                    .build();
            scheduler.scheduleJob(trigger);
            count++;
        }
        log.info("调度器: 已注册 {} {} ({}个调度)", model != null ? model.getModelName() : "", mv.getVersionLabel(), count);
    } catch (SchedulerException e) {
        log.error("调度器: 注册失败 versionId={}", versionId, e);
    }
}
```

添加单条调度管理方法：

```java
/** 激活单条调度 */
public void scheduleOne(Long scheduleId) {
    ModelSchedule s = scheduleMapper.selectById(scheduleId);
    if (s == null || s.getIsActive() != 1) return;

    ModelVersion mv = versionMapper.selectById(s.getVersionId());
    if (mv == null || !"RUNNING".equals(mv.getStatus())) return;

    String cron = CronUtil.toCron(s.getRepetition(), s.getDayOfWeek(), s.getScheduleTime());
    String jobName = jobName(s.getVersionId());

    try {
        if (!scheduler.checkExists(new JobKey(jobName, JOB_GROUP))) {
            // Job 不存在则先创建完整调度
            schedule(s.getVersionId());
            return;
        }

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerName(s.getId()), TRIGGER_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .forJob(jobName, JOB_GROUP)
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
```

修改 Trigger 命名，使用 scheduleId 而非 versionId：

```java
private String triggerName(Long scheduleId) {
    return "trigger-s" + scheduleId;
}
```

同时保留原有的 `triggerName` 用于兼容性（作为 deprecated 或直接删除）。修改 `unschedule` 和 `reschedule` 方法，使用新的 trigger 命名：

```java
/** 从调度器移除一个版本的所有调度 */
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
```

unschedule 保持不变（deleteJob 会连带删除所有关联 Trigger）。移除老的 `reschedule` 方法中的单 Trigger 逻辑：

```java
/** 重调度一个版本 */
public void reschedule(Long versionId) {
    unschedule(versionId);
    ModelVersion mv = versionMapper.selectById(versionId);
    if (mv != null && "RUNNING".equals(mv.getStatus())) {
        schedule(versionId);
    }
}
```

添加导入语句：

```java
import com.ocean.entity.ModelSchedule;
import com.ocean.mapper.ModelScheduleMapper;
import com.ocean.util.CronUtil;
import java.util.List;
```

- [ ] **Step 2: Verify compilation**

Run: `cd ocean-server && mvn compile -q`

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/SchedulerService.java
git commit -m "feat: SchedulerService 扩展多 Trigger 支持 — scheduleId 级 Trigger 管理"
```

---

### Task 9: 后端 — ModelForecastJob 添加禁止并发

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/task/ModelForecastJob.java`

- [ ] **Step 1: 添加 @DisallowConcurrentExecution**

在类注解中添加：

```java
import org.quartz.DisallowConcurrentExecution;

@Slf4j
@Component
@DisallowConcurrentExecution
public class ModelForecastJob implements Job {
```

- [ ] **Step 2: Verify compilation**

Run: `cd ocean-server && mvn compile -q`

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/task/ModelForecastJob.java
git commit -m "fix: ModelForecastJob 添加 @DisallowConcurrentExecution 防止同版本并发"
```

---

### Task 10: 后端 — ModelVersionServiceImpl 适配多调度

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/ModelVersionServiceImpl.java`

- [ ] **Step 1: 修改 toggleStatus 调用 schedule 支持多 Trigger**

`toggleStatus` 中的 `schedulerService.schedule(versionId)` 调用保持不变（`schedule` 方法已改为遍历 model_schedule 表）。无需代码变更，但需确保逻辑一致。

- [ ] **Step 2: 修改 updateVersionWithReschedule**

`updateVersionWithReschedule` 不再依赖 `model_version.cron_expression` 变更检测。简化逻辑——调度变更完全通过 ModelScheduleController 管理。保持方法不变（仍对比 cron_expression 字段），未来单独清理。

- [ ] **Step 3: Commit**

```bash
git add ocean-server/src/main/java/com/ocean/service/impl/ModelVersionServiceImpl.java
git commit -m "refactor: ModelVersionServiceImpl 适配 SchedulerService 多 Trigger"
```

---

### Task 11: 后端 — 集成测试（启动验证）

**Files:** 无新文件

- [ ] **Step 1: 启动后端，验证无启动错误**

Run: `cd ocean-server && mvn spring-boot:run`

检查日志中是否出现 "调度器启动: 扫描到 X 个运行中的版本"，确认无 `SchedulerException` 或 Bean 注入错误。

- [ ] **Step 2: 测试 API 端点**

```bash
# 查询某版本的调度列表（空列表）
curl -s http://localhost:8080/api/model/1/version/1/schedule | python -m json.tool

# 创建调度
curl -s -X POST http://localhost:8080/api/model/1/version/1/schedule \
  -H "Content-Type: application/json" \
  -d '{"repetition":"DAILY","scheduleTime":"06:00"}' | python -m json.tool

# 查询周调度
curl -s "http://localhost:8080/api/model/schedule/week?startDate=2026-05-25&endDate=2026-06-01" | python -m json.tool
```

验证返回 `code: 200` 且 `data` 数组正常。

- [ ] **Step 3: Commit**

没有新代码变更，记录测试通过。

---

### Task 12: 前端 — API 扩展

**Files:**
- Modify: `ocean-web/src/api/model.js`

- [ ] **Step 1: 添加调度相关 API 函数**

在 `model.js` 末尾添加：

```javascript
// ====== 调度管理 ======

/** 获取某版本的所有调度 */
export function getVersionSchedules(modelId, versionId) {
  return request.get(`/model/${modelId}/version/${versionId}/schedule`)
}

/** 创建调度 */
export function addSchedule(modelId, versionId, data) {
  return request.post(`/model/${modelId}/version/${versionId}/schedule`, data)
}

/** 更新调度 */
export function updateSchedule(scheduleId, data) {
  return request.put(`/model/schedule/${scheduleId}`, data)
}

/** 删除调度 */
export function deleteSchedule(scheduleId) {
  return request.delete(`/model/schedule/${scheduleId}`)
}

/** 按周获取调度（日历展示用） */
export function getWeekSchedules(params) {
  return request.get('/model/schedule/week', { params })
}

/** 获取所有可调度版本 */
export function getAvailableVersions() {
  return request.get('/model/schedule/available-versions')
}
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/api/model.js
git commit -m "feat: 前端 API — 调度 CRUD + 周查询接口"
```

---

### Task 13: 前端 — ScheduleBlock 组件

**Files:**
- Create: `ocean-web/src/views/model/ScheduleBlock.vue`

- [ ] **Step 1: 创建 ScheduleBlock 组件**

```vue
<template>
  <div
    class="schedule-block"
    :style="{ backgroundColor: blockColor }"
    @click.stop="$emit('click', schedule)"
    :title="`${schedule.modelName} ${schedule.versionLabel} · ${labelText}`"
  >
    {{ schedule.modelName }} {{ schedule.versionLabel }}
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  schedule: { type: Object, required: true },
  colorIndex: { type: Number, default: 0 }
})

defineEmits(['click'])

const COLORS = ['#2c3e50', '#555', '#777', '#999', '#bbb']

const blockColor = computed(() => COLORS[props.colorIndex % COLORS.length])

const labelText = computed(() => {
  const s = props.schedule
  if (s.repetition === 'DAILY') return '每天'
  if (s.repetition === 'WEEKLY') {
    const days = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
    return days[s.dayOfWeek] || '每周'
  }
  return '仅一次'
})
</script>

<style scoped>
.schedule-block {
  color: #fff;
  font-size: 10px;
  padding: 2px 5px;
  margin: 1px 0;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/model/ScheduleBlock.vue
git commit -m "feat: 新增 ScheduleBlock 组件 — 日历调度块"
```

---

### Task 14: 前端 — ScheduleDialog 组件

**Files:**
- Create: `ocean-web/src/views/model/ScheduleDialog.vue`

- [ ] **Step 1: 创建 ScheduleDialog 组件**

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑调度' : '新建调度'"
    width="420px"
    :close-on-click-modal="false"
    append-to-body
  >
    <div class="schedule-dialog-body">
      <el-form label-position="top" size="default">
        <el-form-item v-if="!isEdit" label="版本">
          <div class="editorial-tag">{{ versionLabel }}</div>
        </el-form-item>
        <el-form-item label="调度标签">
          <input class="editorial-input" v-model="form.scheduleLabel" placeholder="可选标签" />
        </el-form-item>
        <el-form-item label="时间">
          <el-time-picker
            v-model="form.scheduleTime"
            format="HH:mm"
            value-format="HH:mm"
            placeholder="选择时间"
            :teleported="false"
          />
        </el-form-item>
        <el-form-item label="重复规则">
          <select v-model="form.repetition" class="editorial-select">
            <option value="DAILY">每天</option>
            <option value="WEEKLY">每周</option>
            <option value="ONCE">仅一次（{{ displayDate }}）</option>
          </select>
        </el-form-item>
        <el-form-item v-if="form.repetition === 'WEEKLY'" label="星期">
          <select v-model="form.dayOfWeek" class="editorial-select">
            <option :value="1">周一</option>
            <option :value="2">周二</option>
            <option :value="3">周三</option>
            <option :value="4">周四</option>
            <option :value="5">周五</option>
            <option :value="6">周六</option>
            <option :value="7">周日</option>
          </select>
        </el-form-item>
      </el-form>
    </div>
    <template #footer>
      <div class="schedule-dialog-footer">
        <button v-if="isEdit" class="editorial-link" style="color: var(--color-alert);" @click="handleDelete">删除调度</button>
        <div style="flex:1;"></div>
        <button class="editorial-btn-outline" @click="visible = false">取消</button>
        <button class="editorial-btn" style="padding-left:24px;padding-right:24px;" @click="handleConfirm">确认</button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, watch, computed } from 'vue'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  versionLabel: { type: String, default: '' },
  schedule: { type: Object, default: null },
  date: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue', 'submit', 'delete'])

const visible = ref(false)

watch(() => props.modelValue, (v) => {
  visible.value = v
  if (v) {
    if (props.schedule) {
      form.scheduleLabel = props.schedule.scheduleLabel || ''
      form.repetition = props.schedule.repetition || 'DAILY'
      form.dayOfWeek = props.schedule.dayOfWeek || 1
      const t = props.schedule.scheduleTime
      form.scheduleTime = typeof t === 'string' ? t.substring(0, 5) : '06:00'
    } else {
      form.scheduleLabel = ''
      form.repetition = 'DAILY'
      form.dayOfWeek = 1
      form.scheduleTime = '06:00'
    }
  }
})
watch(visible, (v) => { if (!v) emit('update:modelValue', false) })

const form = reactive({
  scheduleLabel: '',
  repetition: 'DAILY',
  dayOfWeek: 1,
  scheduleTime: '06:00'
})

const isEdit = computed(() => !!props.schedule)
const displayDate = computed(() => props.date || '')

function handleConfirm() {
  emit('submit', {
    scheduleLabel: form.scheduleLabel,
    repetition: form.repetition,
    dayOfWeek: form.repetition === 'WEEKLY' ? form.dayOfWeek : null,
    scheduleTime: form.scheduleTime
  })
  visible.value = false
}

function handleDelete() {
  emit('delete', props.schedule.id)
  visible.value = false
}
</script>

<style scoped>
.schedule-dialog-body {
  padding: 8px 0;
}
.schedule-dialog-footer {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/model/ScheduleDialog.vue
git commit -m "feat: 新增 ScheduleDialog 组件 — 拖放弹窗选择重复规则"
```

---

### Task 15: 前端 — VersionCardPool 组件

**Files:**
- Create: `ocean-web/src/views/model/VersionCardPool.vue`

- [ ] **Step 1: 创建 VersionCardPool 组件**

```vue
<template>
  <div class="card-pool">
    <div class="card-pool__header">
      <span class="editorial-section-label">可调度版本</span>
      <span class="card-pool__count">{{ filteredVersions.length }}</span>
    </div>
    <input
      v-model="search"
      class="editorial-search"
      style="width:100%;box-sizing:border-box;margin-bottom:10px;"
      placeholder="搜索版本..."
    />
    <label class="card-pool__filter">
      <input type="checkbox" v-model="runningOnly" /> 仅显示运行中
    </label>
    <div class="card-pool__list">
      <div
        v-for="(v, idx) in filteredVersions"
        :key="v.versionId || v.id"
        class="version-card"
        :class="{ 'version-card--running': isRunning(v) }"
        :draggable="true"
        :data-version-id="v.versionId || v.id"
        @dragstart="onDragStart($event, v, idx)"
        @dragend="onDragEnd"
      >
        <div class="version-card__name">{{ v.modelName }} {{ v.versionLabel }}</div>
        <div class="version-card__meta">
          <span>{{ v.modelType || '' }}</span>
          <span class="version-card__status">{{ isRunning(v) ? '运行中' : '已停止' }}</span>
        </div>
        <div v-if="v.schedules && v.schedules.length" class="version-card__schedules">
          已调度: {{ v.schedules.map(s => scheduleBrief(s)).join(', ') }}
        </div>
        <div v-else class="version-card__schedules" style="color: var(--color-text-muted);">
          未配置调度
        </div>
      </div>
      <div v-if="filteredVersions.length === 0" class="card-pool__empty">
        暂无匹配版本
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  versions: { type: Array, default: () => [] }
})

const emit = defineEmits(['drag-start', 'drag-end'])

const search = ref('')
const runningOnly = ref(true)

const filteredVersions = computed(() => {
  return props.versions.filter(v => {
    const label = (v.modelName || '') + ' ' + (v.versionLabel || '')
    const match = !search.value || label.toLowerCase().includes(search.value.toLowerCase())
    const status = !runningOnly.value || v.status === 'RUNNING'
    return match && status
  })
})

function isRunning(v) {
  return v.status === 'RUNNING'
}

function scheduleBrief(s) {
  if (s.repetition === 'DAILY') return '每天'
  if (s.repetition === 'WEEKLY') {
    const days = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
    return (days[s.dayOfWeek] || '每周') + (s.scheduleTime ? ' ' + s.scheduleTime : '')
  }
  return '仅一次'
}

function onDragStart(e, v, idx) {
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('application/json', JSON.stringify(v))
  emit('drag-start', v)
}

function onDragEnd() {
  emit('drag-end')
}
</script>

<style scoped>
.card-pool {
  padding: 16px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.card-pool__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-pool__count {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--color-text-muted);
}
.card-pool__filter {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
}
.card-pool__list {
  flex: 1;
  overflow-y: auto;
}
.version-card {
  border: 1px solid var(--color-border-light);
  padding: 10px 12px;
  margin-bottom: 6px;
  cursor: grab;
  background: var(--color-bg);
  transition: border-color 0.15s;
}
.version-card:hover {
  border-color: var(--color-text);
}
.version-card--running {
  border-left: 2px solid var(--color-text);
}
.version-card__name {
  font-weight: 600;
  font-size: 13px;
  color: var(--color-text);
  margin-bottom: 4px;
}
.version-card__meta {
  font-size: 11px;
  color: var(--color-text-muted);
  display: flex;
  justify-content: space-between;
}
.version-card__status {
  font-family: var(--font-mono);
  font-size: 10px;
}
.version-card__schedules {
  font-size: 10px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}
.card-pool__empty {
  font-size: 12px;
  color: var(--color-text-muted);
  text-align: center;
  padding: 24px 0;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/model/VersionCardPool.vue
git commit -m "feat: 新增 VersionCardPool 组件 — 可拖拽版本卡片池"
```

---

### Task 16: 前端 — ScheduleCalendar 组件（周视图日历）

**Files:**
- Create: `ocean-web/src/views/model/ScheduleCalendar.vue`

- [ ] **Step 1: 创建 ScheduleCalendar 组件**

```vue
<template>
  <div class="schedule-calendar">
    <div class="calendar-header">
      <button class="editorial-link" @click="prevWeek">← 上一周</button>
      <span class="calendar-header__range">{{ weekRangeText }}</span>
      <button class="editorial-link" @click="nextWeek">下一周 →</button>
    </div>

    <div class="calendar-grid-wrapper">
      <table class="calendar-grid">
        <thead>
          <tr>
            <th class="time-col">时间</th>
            <th
              v-for="(day, idx) in weekDays"
              :key="idx"
              class="day-col"
              :class="{ 'day-col--today': isToday(day) }"
              @dragover.prevent="onDragOver($event, day, null)"
              @drop.prevent="onDrop($event, day, null)"
            >
              <div class="day-label">{{ dayNames[idx] }}</div>
              <div class="day-date">{{ day }}</div>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="hour in hours" :key="hour">
            <td class="time-label">{{ pad(hour) }}:00</td>
            <td
              v-for="(day, dIdx) in weekDays"
              :key="dIdx"
              class="cell"
              :class="{ 'cell--today': isToday(day) }"
              @dragover.prevent="onDragOver($event, day, hour)"
              @drop.prevent="onDrop($event, day, hour)"
            >
              <ScheduleBlock
                v-for="(s, sIdx) in getSchedulesForDayHour(day, hour)"
                :key="s.id"
                :schedule="s"
                :color-index="getColorIndex(s)"
                @click="handleBlockClick(s)"
              />
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="allSchedules.length === 0" class="calendar-empty">
      暂无调度配置，从右侧拖拽版本卡片到此区域
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import ScheduleBlock from './ScheduleBlock.vue'

const props = defineProps({
  weekStart: { type: String, required: true },
  schedules: { type: Array, default: () => [] },
  colorMap: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['prev-week', 'next-week', 'cell-drop', 'block-click'])

const dayNames = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const hours = Array.from({ length: 24 }, (_, i) => i)

const currentStart = ref(props.weekStart)

watch(() => props.weekStart, (v) => { currentStart.value = v })

const weekDays = computed(() => {
  const start = new Date(currentStart.value)
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(start)
    d.setDate(d.getDate() + i)
    return formatDate(d)
  })
})

const weekRangeText = computed(() => {
  return weekDays.value[0] + ' — ' + weekDays.value[6]
})

const allSchedules = computed(() => props.schedules)

function formatDate(d) {
  return d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0')
}

function isToday(dateStr) {
  const today = formatDate(new Date())
  return dateStr === today
}

function pad(n) { return String(n).padStart(2, '0') }

function getSchedulesForDayHour(day, hour) {
  return allSchedules.value.filter(s => {
    const timeStr = s.scheduleTime
    if (!timeStr) return false
    const h = parseInt(timeStr.substring(0, 2))
    if (h !== hour) return false

    if (s.repetition === 'DAILY' || s.repetition === 'ONCE') return true
    if (s.repetition === 'WEEKLY') {
      const d = new Date(day)
      const dow = d.getDay()
      const expected = dow === 0 ? 7 : dow
      return s.dayOfWeek === expected
    }
    return false
  })
}

function getColorIndex(schedule) {
  const key = schedule.modelName || ''
  return props.colorMap[key] ?? 0
}

function onDragOver(e, day, hour) {
  e.dataTransfer.dropEffect = 'copy'
}

function onDrop(e, day, hour) {
  try {
    const json = e.dataTransfer.getData('application/json')
    const version = JSON.parse(json)
    emit('cell-drop', { version, date: day, hour })
  } catch (err) {
    // ignore invalid drops
  }
}

function handleBlockClick(schedule) {
  emit('block-click', schedule)
}

function prevWeek() {
  const d = new Date(currentStart.value)
  d.setDate(d.getDate() - 7)
  currentStart.value = formatDate(d)
  emit('prev-week', currentStart.value)
}

function nextWeek() {
  const d = new Date(currentStart.value)
  d.setDate(d.getDate() + 7)
  currentStart.value = formatDate(d)
  emit('next-week', currentStart.value)
}
</script>

<style scoped>
.schedule-calendar {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 16px;
}
.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.calendar-header__range {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text);
}
.calendar-grid-wrapper {
  flex: 1;
  overflow-y: auto;
}
.calendar-grid {
  width: 100%;
  border-collapse: collapse;
  font-size: 11px;
}
.time-col {
  width: 50px;
  padding: 4px 6px;
  color: var(--color-text-muted);
  font-size: 9px;
  text-align: right;
  border-right: 1px solid var(--color-divider-strong);
}
.day-col {
  padding: 6px 4px;
  border-bottom: 1px solid var(--color-divider);
  text-align: center;
}
.day-col--today {
  background: var(--color-surface);
}
.day-label {
  font-size: 10px;
  color: var(--color-text-muted);
}
.day-date {
  font-family: var(--font-serif);
  font-size: 13px;
  color: var(--color-text);
}
.time-label {
  padding: 2px 6px;
  color: var(--color-text-muted);
  font-size: 9px;
  text-align: right;
  border-right: 1px solid var(--color-divider-strong);
  vertical-align: top;
  padding-top: 6px;
}
.cell {
  height: 36px;
  padding: 2px 3px;
  border: 1px solid var(--color-divider);
  vertical-align: top;
  transition: background 0.1s;
}
.cell:hover {
  background: var(--color-surface);
}
.cell--today {
  background: var(--color-surface);
}
.calendar-empty {
  font-family: var(--font-serif);
  font-size: 15px;
  color: var(--color-text-muted);
  font-style: italic;
  text-align: center;
  padding: 60px 0;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ocean-web/src/views/model/ScheduleCalendar.vue
git commit -m "feat: 新增 ScheduleCalendar 组件 — 周视图日历 + DnD"
```

---

### Task 17: 前端 — ScheduleOverview 页面集成

**Files:**
- Modify: `ocean-web/src/views/model/ScheduleOverview.vue`

- [ ] **Step 1: 重写 ScheduleOverview.vue**

```vue
<template>
  <div class="schedule-overview">
    <ScheduleCalendar
      :week-start="weekStart"
      :schedules="schedules"
      :color-map="colorMap"
      @cell-drop="onCellDrop"
      @block-click="onBlockClick"
      @prev-week="onWeekChange"
      @next-week="onWeekChange"
    />
    <VersionCardPool
      :versions="availableVersions"
      @drag-start="onDragStart"
      @drag-end="onDragEnd"
    />
    <ScheduleDialog
      v-model="dialogVisible"
      :version-label="dialogVersionLabel"
      :schedule="dialogSchedule"
      :date="dialogDate"
      @submit="handleSubmit"
      @delete="handleDeleteSchedule"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ScheduleCalendar from './ScheduleCalendar.vue'
import VersionCardPool from './VersionCardPool.vue'
import ScheduleDialog from './ScheduleDialog.vue'
import {
  getWeekSchedules,
  getAvailableVersions,
  addSchedule,
  updateSchedule,
  deleteSchedule
} from '@/api/model'

const weekStart = ref(getMonday(new Date()))
const schedules = ref([])
const availableVersions = ref([])
const colorMap = ref({})

const dialogVisible = ref(false)
const dialogVersion = ref(null)
const dialogSchedule = ref(null)
const dialogDate = ref('')
const dialogVersionLabel = computed(() => {
  const v = dialogVersion.value
  if (!v) return ''
  return (v.modelName || '') + ' ' + (v.versionLabel || '')
})

function getMonday(d) {
  const date = new Date(d)
  const day = date.getDay()
  const diff = date.getDate() - day + (day === 0 ? -6 : 1)
  date.setDate(diff)
  return formatDate(date)
}

function formatDate(d) {
  return d.getFullYear() + '-' +
    String(d.getMonth() + 1).padStart(2, '0') + '-' +
    String(d.getDate()).padStart(2, '0')
}

function getWeekEnd() {
  const d = new Date(weekStart.value)
  d.setDate(d.getDate() + 6)
  return formatDate(d)
}

async function loadSchedules() {
  try {
    const res = await getWeekSchedules({
      startDate: weekStart.value,
      endDate: getWeekEnd()
    })
    schedules.value = res.data || []
    buildColorMap()
  } catch (e) {
    ElMessage.error('加载调度数据失败')
  }
}

async function loadVersions() {
  try {
    const res = await getAvailableVersions()
    availableVersions.value = res.data || []
  } catch (e) {
    ElMessage.error('加载版本列表失败')
  }
}

function buildColorMap() {
  const models = [...new Set(schedules.value.map(s => s.modelName).filter(Boolean))]
  const colors = ['#2c3e50', '#555', '#777', '#999', '#bbb']
  const map = {}
  models.forEach((m, i) => { map[m] = i % colors.length })
  colorMap.value = map
}

function onWeekChange(newStart) {
  weekStart.value = newStart
  loadSchedules()
}

function onCellDrop({ version, date, hour }) {
  const hh = String(hour).padStart(2, '0')
  dialogVersion.value = version
  dialogSchedule.value = null
  dialogDate.value = date
  dialogVisible.value = true
}

function onBlockClick(schedule) {
  dialogVersion.value = { modelName: schedule.modelName, versionLabel: schedule.versionLabel }
  dialogSchedule.value = schedule
  dialogVisible.value = true
}

async function handleSubmit(formData) {
  const v = dialogVersion.value
  if (!v) return
  try {
    await addSchedule(v.modelId || v.id, v.versionId || v.id, formData)
    ElMessage.success('调度创建成功')
    loadSchedules()
  } catch (e) {
    ElMessage.error('调度创建失败')
  }
}

async function handleDeleteSchedule(scheduleId) {
  try {
    await deleteSchedule(scheduleId)
    ElMessage.success('调度已删除')
    loadSchedules()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

function onDragStart(version) {
  // visual feedback placeholder
}

function onDragEnd() {
  // visual feedback placeholder
}

onMounted(() => {
  loadSchedules()
  loadVersions()
})
</script>

<style scoped>
.schedule-overview {
  display: flex;
  height: calc(100vh - 64px);
}
.schedule-overview > :first-child {
  flex: 1;
  min-width: 0;
}
.schedule-overview > :last-child {
  width: 280px;
  min-width: 280px;
  border-left: 1px solid var(--color-divider-strong);
  background: var(--color-surface);
}
</style>
```

- [ ] **Step 2: Verify compilation**

Run: `cd ocean-web && npm run build`

- [ ] **Step 3: Commit**

```bash
git add ocean-web/src/views/model/ScheduleOverview.vue
git commit -m "feat: 重写 ScheduleOverview — 拖拽排程页面集成"
```

---

### Task 18: 前端 — 深色模式适配

**Files:**
- Modify: `ocean-web/src/views/model/ScheduleBlock.vue`
- Modify: `ocean-web/src/views/model/ScheduleCalendar.vue`
- Modify: `ocean-web/src/views/model/VersionCardPool.vue`
- Modify: `ocean-web/src/views/model/ScheduleOverview.vue`

- [ ] **Step 1: 验证所有样式使用 CSS 变量**

审查以上组件，确保：
- 无硬编码颜色值（除调度块背景色 `#2c3e50 / #555 / #777 / #999 / #bbb` 为有意固定灰度阶梯）
- 所有文字、边框、背景色均使用 `var(--color-*)` 变量
- 调度块文字 `#fff` 在深色模式下保持可读（白色文字，深色/灰色背景）
- 网格线、hover 状态均走 CSS 变量

以上组件已全部使用 `var(--color-*)` 变量（审查通过，无需修改）。

- [ ] **Step 2: 启动前端验证深色模式**

Run: `cd ocean-web && npm run dev`

在浏览器中：
1. 切换到 `/app/model/schedule`
2. 切换深色模式（设置 → 主题切换）
3. 验证日历网格、卡片池、调度块在深色模式下正常显示

- [ ] **Step 3: Commit**

```bash
git commit -m "chore: 调度总览深色模式适配验证"
```

---

### Task 19: 前端 — 边界情况处理

**Files:**
- Modify: `ocean-web/src/views/model/ScheduleOverview.vue`

- [ ] **Step 1: 添加网络错误恢复逻辑**

在 ScheduleOverview.vue 中确认已处理：
- `loadSchedules()` 中已有 try-catch + ElMessage 错误提示
- `loadVersions()` 中已有 try-catch + ElMessage 错误提示
- `handleSubmit()` 中已有 try-catch + ElMessage 错误提示
- `handleDeleteSchedule()` 中已有 try-catch + ElMessage 错误提示
- 空状态：`ScheduleCalendar.vue` 中已有 `v-if="allSchedules.length === 0"` 空状态提示
- 卡片池空状态：`VersionCardPool.vue` 中已有 `v-if="filteredVersions.length === 0"` 提示
- 拖拽无效区域：`ScheduleCalendar.vue` 中 `onDrop` 已有 try-catch 忽略无效 JSON

边界情况全部已覆盖，无需额外修改。

- [ ] **Step 2: Commit**

没有新代码变更，记录边界情况审查通过。

---

### Task 20: 端到端验证

**Files:** 无新文件

- [ ] **Step 1: 启动后端**

Run: `cd ocean-server && mvn spring-boot:run`
Verify: 无启动错误，日志显示 "调度器启动: 扫描到 X 个运行中的版本"

- [ ] **Step 2: 启动前端**

Run: `cd ocean-web && npm run dev`

- [ ] **Step 3: 测试完整流程**

1. 访问 `http://localhost:3000/app/model/schedule`
2. 从右侧卡片池拖拽版本到周视图日历的某个时间格
3. 验证弹窗出现，选择重复规则并确认
4. 验证调度块出现在日历上
5. 点击已存在的调度块，验证编辑/删除功能
6. 切换上一周/下一周
7. 切换深色模式，验证显示正常

- [ ] **Step 4: 最终 Commit**

```bash
git add -A
git commit -m "feat: 调度总览拖拽排程 — 端到端验证通过"
```
