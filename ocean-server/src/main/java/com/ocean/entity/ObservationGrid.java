package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("observation_grid")
public class ObservationGrid {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String variable;

    private LocalDate obsDate;

    private Double depth;

    private Double lat;

    private Double lon;

    private Double value;

    private String unit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
