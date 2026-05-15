package com.ocean.vo;

import lombok.Data;

/**
 * 运行中版本视图对象（概览用）
 */
@Data
public class RunningVersionVO {

    private Long versionId;

    private Long modelId;

    private String modelName;

    private String versionLabel;
}
