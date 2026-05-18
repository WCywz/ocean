package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.StationSaveDTO;
import com.ocean.vo.StationVO;
import java.util.List;

public interface StationService {
    IPage<StationVO> getStationPage(Integer pageNum, Integer pageSize, String distance, String region);
    StationVO getStationById(Long id);
    List<StationVO> getStationsByZoneId(Long zoneId);
    void addStation(StationSaveDTO dto);
    void updateStation(StationSaveDTO dto);
    void deleteStation(Long id);
}
