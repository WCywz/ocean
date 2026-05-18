package com.ocean.vo;

import lombok.Data;

@Data
public class AlertStationDetailVO {
    private Long id;
    private Long alertId;
    private Long stationId;
    private String stationName;
    private Double lat;
    private Double lon;
    private Double actualValue;
    private Double threshold;
}
