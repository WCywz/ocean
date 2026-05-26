package com.ocean.service;

import com.ocean.entity.HealthZone;
import com.ocean.vo.ZoneHealthVO;
import com.ocean.vo.ZoneHealthV2VO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HealthService {
    List<HealthZone> getZones();
    ZoneHealthVO getAssessment(LocalDate date);
    List<Map<String, Object>> getZoneTrend(Long zoneId, LocalDate startDate, LocalDate endDate);
    Map<String, Object> getDashboard();

    /**
     * Build daily SMS summary text for all zones.
     * @return formatted SMS message body, or null if no data available
     */
    String buildDailySummary();

    ZoneHealthV2VO getAssessmentV2(LocalDate date, int lookback, int lookahead);

    Map<String, Object> getAlertMap(LocalDate date);
}
