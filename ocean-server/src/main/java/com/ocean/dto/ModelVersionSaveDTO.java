package com.ocean.dto;

import lombok.Data;

/**
 * 模型版本新增/修改参数
 */
@Data
public class ModelVersionSaveDTO {

    private Long id;

    private String versionLabel;

    private String cronExpression;

    private String paramsConfig;

    private String dataSource;

    private String dataTimeRange;

    private String changeNote;
}
