package com.ocean.vo;

import lombok.Data;

@Data
public class StationVO {
    private Long id;
    private String stationName;
    private Double lat;
    private Double lon;
    private String distance;
    private String region;
    private Long healthZoneId;
    private String zoneName;
    private Integer isActive;
    private Integer sortOrder;
}
