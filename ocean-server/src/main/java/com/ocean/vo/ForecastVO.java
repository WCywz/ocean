package com.ocean.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 预报数据返回
 */
@Data
public class ForecastVO {

    private Long id;
    private Long modelId;
    private String modelName;
    private String dataType;
    private LocalDate forecastDate;
    private String locationName;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Double value;
    private String unit;
    private LocalDateTime createTime;
}
