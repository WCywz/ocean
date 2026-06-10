package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 模型调度配置实体
 */
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

    private LocalDate scheduleDate;

    private Integer isActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
