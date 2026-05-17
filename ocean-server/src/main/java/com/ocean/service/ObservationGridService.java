package com.ocean.service;

import java.util.List;
import java.util.Map;

public interface ObservationGridService {
    List<Map<String, Object>> getMapGrid(String variable, String obsDate,
                                          Double minLon, Double maxLon,
                                          Double minLat, Double maxLat);
    List<Map<String, Object>> getPointTrend(String dataType, Double lon, Double lat,
                                             String dateStart, String dateEnd);
    List<Map<String, Object>> getTrend(String dataType, Double lon, Double lat);
    List<Map<String, Object>> getDistinctLocations();
}
