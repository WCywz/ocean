package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预报数据记录实体
 */
@Data
@TableName("forecast_record")
public class ForecastRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联模型ID */
    private Long modelId;

    /** 数据类型: SST-海表温度, CHL-叶绿素浓度 */
    private String dataType;

    /** 预报日期 */
    private LocalDate forecastDate;

    /** 观测点名称 */
    private String locationName;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 预报数值 */
    private Double value;

    /** 单位 */
    private String unit;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
