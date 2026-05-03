package com.ocean.config;

import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.*;

/**
 * 预设海域 bbox 配置
 * bbox 顺序: [minLon, minLat, maxLon, maxLat]
 */
@Configuration
public class SeaAreaConfig {

    public List<Map<String, Object>> getSeaAreas() {
        List<Map<String, Object>> areas = new ArrayList<>();

        areas.add(area("全部海域",  121.33, 26.92, 125.58, 32.67));
        areas.add(area("北部海域",  121.33, 30.00, 125.58, 32.67));
        areas.add(area("南部海域",  121.33, 26.92, 125.58, 30.00));
        areas.add(area("近岸海域",  121.33, 26.92, 122.50, 32.67));
        areas.add(area("远海海域",  122.50, 26.92, 125.58, 32.67));

        return areas;
    }

    private Map<String, Object> area(String name, double minLon, double minLat, double maxLon, double maxLat) {
        return Map.of(
            "name", name,
            "minLon", BigDecimal.valueOf(minLon),
            "maxLon", BigDecimal.valueOf(maxLon),
            "minLat", BigDecimal.valueOf(minLat),
            "maxLat", BigDecimal.valueOf(maxLat)
        );
    }
}
