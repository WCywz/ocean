package com.ocean.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型版本视图对象
 */
@Data
public class ModelVersionVO {

    private Long id;

    private Long groupId;

    private String versionLabel;

    private String cronExpression;

    private String paramsConfig;

    private String dataSource;

    private String dataTimeRange;

    private String changeNote;

    private String status;

    private LocalDateTime lastRunTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
