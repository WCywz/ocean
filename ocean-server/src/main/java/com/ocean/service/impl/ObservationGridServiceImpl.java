package com.ocean.service.impl;

import com.ocean.mapper.ObservationGridMapper;
import com.ocean.service.ObservationGridService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ObservationGridServiceImpl implements ObservationGridService {

    @Autowired
    private ObservationGridMapper observationGridMapper;

    @Override
    public List<Map<String, Object>> getMapGrid(String variable, String obsDate,
                                                 Double minLon, Double maxLon,
                                                 Double minLat, Double maxLat) {
        return observationGridMapper.selectMapGrid(variable, obsDate, minLon, maxLon, minLat, maxLat);
    }

    @Override
    public List<Map<String, Object>> getPointTrend(String dataType, Double lon, Double lat,
                                                    String dateStart, String dateEnd) {
        return observationGridMapper.selectPointTrend(dataType, lon, lat, dateStart, dateEnd);
    }

    @Override
    public List<Map<String, Object>> getTrend(String dataType, Double lon, Double lat) {
        return observationGridMapper.selectTrend(dataType, lon, lat);
    }

    @Override
    public List<Map<String, Object>> getDistinctLocations() {
        return observationGridMapper.selectDistinctLocations();
    }
}
