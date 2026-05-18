package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 健康分区实体
 */
@Data
@TableName("health_zone")
public class HealthZone {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String zoneName;

    private Double minLon;

    private Double maxLon;

    private Double minLat;

    private Double maxLat;

    private Integer sortOrder;

    private Integer isActive;
}
