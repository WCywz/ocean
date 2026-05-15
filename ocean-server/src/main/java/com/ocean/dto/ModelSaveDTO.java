package com.ocean.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 模型新增/修改参数
 */
@Data
public class ModelSaveDTO {

    private Long id;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "模型类型不能为空")
    private String modelType;

    private String paramsConfig;
    private String cronExpression;
    private String description;
}
