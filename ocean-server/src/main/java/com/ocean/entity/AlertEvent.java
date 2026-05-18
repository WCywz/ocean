package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 告警事件实体
 */
@Data
@TableName("alert_event")
public class AlertEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long zoneId;

    private Long ruleId;

    private String variable;

    private String source;

    private LocalDate alertDate;

    private Double maxValue;

    private Double avgValue;

    private Double threshold;

    private Integer stationCount;

    private String severity;

    private String status;

    private Long ackBy;

    private LocalDateTime ackAt;

    private String message;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
