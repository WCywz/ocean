package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康记录实体
 */
@Data
@TableName("health_record")
public class HealthRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long zoneId;

    private LocalDate assessDate;

    private Double sstAvg;

    private Double sstMax;

    private Double sstAnomaly;

    private String sstTrend;

    private Double chlAvg;

    private Double chlMax;

    private String chlTrend;

    private Integer heatwaveActive;

    private Integer heatwaveDays;

    private String sstGrade;

    private String chlGrade;

    private String heatwaveGrade;

    private String overallGrade;

    private String suggestions;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
