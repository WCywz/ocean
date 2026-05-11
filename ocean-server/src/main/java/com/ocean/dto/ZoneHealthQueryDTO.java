package com.ocean.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ZoneHealthQueryDTO {
    private BigDecimal centerLon;
    private BigDecimal centerLat;
    private BigDecimal coastLon;
    private String forecastDate;
}
