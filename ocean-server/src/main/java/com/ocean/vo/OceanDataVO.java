package com.ocean.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 海洋观测数据返回
 */
@Data
public class OceanDataVO {

    private Long id;
    private BigDecimal lat;
    private BigDecimal lon;
    private LocalDate time;
    private Double chl;
    private Double sst;
    private Double depth;
    private LocalDateTime createTime;
}
