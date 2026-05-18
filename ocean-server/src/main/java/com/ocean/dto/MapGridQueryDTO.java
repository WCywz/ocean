package com.ocean.dto;

import lombok.Data;

@Data
public class MapGridQueryDTO {
    private String dataType;
    private String forecastDate;
    private Double precision;
    private Double minLon;
    private Double maxLon;
    private Double minLat;
    private Double maxLat;
    private Double threshold;
    private String chlMode;
}
