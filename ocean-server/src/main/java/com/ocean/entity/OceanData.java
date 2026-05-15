package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 海洋观测数据实体
 */
@Data
@TableName("ocean_data")
public class OceanData {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 纬度 */
    private BigDecimal lat;

    /** 经度 */
    private BigDecimal lon;

    /** 观测日期 */
    private LocalDate time;

    /** 叶绿素浓度 */
    private Double chl;

    /** 海表温度 (°C) */
    private Double sst;

    /** 深度 */
    private Double depth;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
