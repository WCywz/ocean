package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.common.BusinessException;
import com.ocean.dto.StationSaveDTO;
import com.ocean.entity.HealthZone;
import com.ocean.entity.MonitoringStation;
import com.ocean.mapper.HealthZoneMapper;
import com.ocean.mapper.MonitoringStationMapper;
import com.ocean.service.StationService;
import com.ocean.vo.StationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StationServiceImpl implements StationService {

    @Autowired private MonitoringStationMapper stationMapper;
    @Autowired private HealthZoneMapper healthZoneMapper;

    @Override
    public IPage<StationVO> getStationPage(Integer pageNum, Integer pageSize, String distance, String region) {
        Page<MonitoringStation> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MonitoringStation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MonitoringStation::getIsActive, 1);
        if (distance != null && !distance.isEmpty()) wrapper.eq(MonitoringStation::getDistance, distance);
        if (region != null && !region.isEmpty()) wrapper.eq(MonitoringStation::getRegion, region);
        wrapper.orderByAsc(MonitoringStation::getSortOrder);
        return stationMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    public StationVO getStationById(Long id) {
        MonitoringStation station = stationMapper.selectById(id);
        if (station == null) throw new BusinessException("站点不存在");
        return toVO(station);
    }

    @Override
    public List<StationVO> getStationsByZoneId(Long zoneId) {
        List<MonitoringStation> stations = stationMapper.selectList(
                new LambdaQueryWrapper<MonitoringStation>()
                        .eq(MonitoringStation::getHealthZoneId, zoneId)
                        .eq(MonitoringStation::getIsActive, 1));
        List<StationVO> result = new ArrayList<>();
        for (MonitoringStation s : stations) result.add(toVO(s));
        return result;
    }

    @Override
    public void addStation(StationSaveDTO dto) {
        MonitoringStation station = new MonitoringStation();
        BeanUtils.copyProperties(dto, station);
        station.setIsActive(1);
        stationMapper.insert(station);
    }

    @Override
    public void updateStation(StationSaveDTO dto) {
        MonitoringStation station = stationMapper.selectById(dto.getId());
        if (station == null) throw new BusinessException("站点不存在");
        BeanUtils.copyProperties(dto, station);
        stationMapper.updateById(station);
    }

    @Override
    public void deleteStation(Long id) {
        if (stationMapper.selectById(id) == null) throw new BusinessException("站点不存在");
        stationMapper.deleteById(id);
    }

    private StationVO toVO(MonitoringStation s) {
        StationVO vo = new StationVO();
        BeanUtils.copyProperties(s, vo);
        if (s.getHealthZoneId() != null) {
            HealthZone zone = healthZoneMapper.selectById(s.getHealthZoneId());
            vo.setZoneName(zone != null ? zone.getZoneName() : "");
        }
        return vo;
    }
}
