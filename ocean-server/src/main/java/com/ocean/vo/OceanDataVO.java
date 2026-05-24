package com.ocean.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 海洋观测数据返回
 */
@Data
public class OceanDataVO {

    private Long id;
    private String variable;
    private LocalDate obsDate;
    private Double value;
    private String unit;
    private Double depth;
    private Double lat;
    private Double lon;
    private LocalDateTime createTime;
}
