package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import java.util.List;
import java.util.Map;

public interface ForecastService {
    DashboardVO getDashboard();
    IPage<ForecastVO> getRecordPage(ForecastQueryDTO dto);
    List<Map<String, Object>> getSstTrend(Double lon, Double lat);
    List<Map<String, Object>> getChlTrend(Double lon, Double lat);
    List<Map<String, Object>> getLocations();
    List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto);
    List<Map<String, Object>> getPointTrend(String dataType, Double lon, Double lat, String dateStart, String dateEnd);
    List<Map<String, Object>> getDashboardTrend(String dataType, Integer days);
    List<Map<String, Object>> getChlProbability(String dateStart, String dateEnd, Double threshold);
    List<Map<String, Object>> getSeaAreas();

    /** Run model forecast and persist results to forecast_grid */
    Map<String, Object> runForecast();

    /** Run model forecast for a specific version, filtering by model type */
    Map<String, Object> runForecast(Long versionId, Long modelId, String modelType);
}
