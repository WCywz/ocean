package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 观测数据实体
 */
@Data
@TableName("observation_data")
public class ObservationData {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String variable;

    private LocalDate obsTime;

    private Double depth;

    private Double lat;

    private Double lon;

    private Double value;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
