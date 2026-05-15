package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预报模型组实体
 */
@Data
@TableName("model_group")
public class ModelGroup {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String modelName;

    private String modelType;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
