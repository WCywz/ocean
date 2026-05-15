package com.ocean.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型信息返回
 */
@Data
public class ModelVO {

    private Long id;
    private String modelName;
    private String modelType;
    private String paramsConfig;
    private String cronExpression;
    private String status;
    private LocalDateTime lastRunTime;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
