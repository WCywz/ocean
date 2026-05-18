package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 监测站点实体
 */
@Data
@TableName("monitoring_station")
public class MonitoringStation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String stationName;

    private Double lat;

    private Double lon;

    private String distance;

    private String region;

    private Long healthZoneId;

    private Integer isActive;

    private Integer sortOrder;
}
