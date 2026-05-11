package com.ocean.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ZoneHealthVO {
    private String zoneName;
    private List<SubZone> zones;

    @Data
    public static class SubZone {
        private String id;
        private String label;
        private SstInfo sst;
        private ChlInfo chl;
        private HeatwaveInfo heatwave;
    }

    @Data
    public static class SstInfo {
        private Double avg;
        private Double max;
        private String trend;
        private Double anomaly;
    }

    @Data
    public static class ChlInfo {
        private Double avg;
        private Double max;
        private String trend;
    }

    @Data
    public static class HeatwaveInfo {
        private Boolean active;
        private Integer days;
    }
}
