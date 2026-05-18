package com.ocean.dto;

import lombok.Data;

@Data
public class ForecastQueryDTO {
    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String dataType;
    private String forecastDate;
    private Double minLon;
    private Double maxLon;
    private Double minLat;
    private Double maxLat;
    private String locationName;
    private String forecastDateBegin;
    private String forecastDateEnd;
}
