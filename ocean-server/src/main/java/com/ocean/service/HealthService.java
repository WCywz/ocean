package com.ocean.service;

import com.ocean.entity.HealthZone;
import com.ocean.vo.ZoneHealthVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HealthService {
    List<HealthZone> getZones();
    ZoneHealthVO getAssessment(LocalDate date);
    List<Map<String, Object>> getZoneTrend(Long zoneId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> getDashboard();
}
