package com.ocean.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 模型调度配置视图对象
 */
@Data
public class ModelScheduleVO {
    private Long id;
    private Long versionId;
    private String scheduleLabel;
    private String repetition;
    private Integer dayOfWeek;
    private LocalTime scheduleTime;
    private LocalDate scheduleDate;
    private Integer isActive;
    private String cronExpression;
    private String modelName;
    private String versionLabel;
    private String modelType;
    private LocalDateTime createTime;
}
