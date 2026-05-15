package com.ocean.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型组视图对象
 */
@Data
public class ModelGroupVO {

    private Long id;

    private String modelName;

    private String modelType;

    private String description;

    /** 该模型组下的版本总数 */
    private Long versionCount;

    /** 该模型组下运行中的版本数 */
    private Long runningCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
