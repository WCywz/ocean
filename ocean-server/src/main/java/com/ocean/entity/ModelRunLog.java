package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("model_run_log")
public class ModelRunLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long versionId;

    private Long modelId;

    private String modelName;

    private String versionLabel;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    private String status;

    private String errorMessage;

    private String outputSummary;

    private String logText;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
