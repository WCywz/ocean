package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预报网格数据实体
 */
@Data
@TableName("forecast_grid")
public class ForecastGrid {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long modelId;

    private Long versionId;

    private String variable;

    private LocalDate forecastDate;

    private Double depth;

    private Double lat;

    private Double lon;

    private Double value;

    private String unit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
