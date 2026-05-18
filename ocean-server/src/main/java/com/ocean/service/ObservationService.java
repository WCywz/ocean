package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.vo.OceanDataVO;
import java.util.List;
import java.util.Map;

public interface ObservationService {
    IPage<OceanDataVO> getDataPage(OceanDataQueryDTO dto);
    List<Map<String, Object>> getSstTimeSeries(String startDate, String endDate, Double lat, Double lon);
    List<Map<String, Object>> getChlTimeSeries(String startDate, String endDate, Double lat, Double lon);
    List<Map<String, Object>> getChlByDepth();
    List<Map<String, Object>> getDistinctLocations();
}
