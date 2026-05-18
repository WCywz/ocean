package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型版本实体
 */
@Data
@TableName("model_version")
public class ModelVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long modelId;

    private String versionLabel;

    private String paramsConfig;

    private String cronExpression;

    private String dataSource;

    private String dataTimeRange;

    private String changeNote;

    private String status;

    private LocalDateTime lastRunTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
