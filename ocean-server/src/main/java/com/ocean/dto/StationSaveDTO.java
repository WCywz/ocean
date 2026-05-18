package com.ocean.dto;

import lombok.Data;

@Data
public class StationSaveDTO {
    private Long id;
    private String stationName;
    private Double lat;
    private Double lon;
    private String distance;
    private String region;
    private Long healthZoneId;
    private Integer sortOrder;
}
