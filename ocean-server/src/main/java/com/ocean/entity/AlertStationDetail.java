package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 告警站点详情实体
 */
@Data
@TableName("alert_station_detail")
public class AlertStationDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long alertId;

    private Long stationId;

    private Double actualValue;

    private Double threshold;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
