package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.entity.*;
import com.ocean.mapper.*;
import com.ocean.service.ForecastService;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import com.ocean.service.SystemConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ForecastServiceImpl implements ForecastService {

    @Autowired private ForecastGridMapper forecastGridMapper;
    @Autowired private ModelMapper modelMapper;
    @Autowired private ModelVersionMapper modelVersionMapper;
    @Autowired private AlertEventMapper alertEventMapper;
    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private SystemConfigService systemConfigService;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setModelCount(modelMapper.selectCount(null));
        vo.setRunningModelCount(modelVersionMapper.selectCount(
                new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getStatus, "RUNNING")));
        vo.setTodayRecordCount(forecastGridMapper.selectCount(
                new LambdaQueryWrapper<ForecastGrid>().eq(ForecastGrid::getForecastDate, systemConfigService.getSystemDate())));
        vo.setAlertCount(alertEventMapper.selectCount(
                new LambdaQueryWrapper<AlertEvent>().eq(AlertEvent::getStatus, "active")));
        vo.setLatestSstData(getLatestGridData("sst"));
        vo.setLatestChlData(getLatestGridData("chl"));
        return vo;
    }

    private List<Map<String, Object>> getLatestGridData(String variable) {
        List<Map<String, Object>> result = new ArrayList<>();
        LambdaQueryWrapper<ForecastGrid> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForecastGrid::getVariable, variable)
               .eq(ForecastGrid::getDepth, 0)
               .orderByDesc(ForecastGrid::getForecastDate)
               .last("LIMIT 5");
        List<ForecastGrid> list = forecastGridMapper.selectList(wrapper);
        for (ForecastGrid g : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("lat", g.getLat());
            m.put("lon", g.getLon());
            m.put("value", g.getValue());
            m.put("forecastDate", g.getForecastDate().toString());
            result.add(m);
        }
        return result;
    }

    @Override
    public IPage<ForecastVO> getRecordPage(ForecastQueryDTO dto) {
        Page<ForecastGrid> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ForecastGrid> wrapper = new LambdaQueryWrapper<>();
        if (dto.getDataType() != null && !dto.getDataType().isEmpty())
            wrapper.eq(ForecastGrid::getVariable, dto.getDataType().toLowerCase());
        if (dto.getForecastDate() != null && !dto.getForecastDate().isEmpty())
            wrapper.eq(ForecastGrid::getForecastDate, dto.getForecastDate());
        wrapper.orderByDesc(ForecastGrid::getForecastDate);
        IPage<ForecastGrid> dataPage = forecastGridMapper.selectPage(page, wrapper);
        return dataPage.convert(this::toForecastVO);
    }

    private ForecastVO toForecastVO(ForecastGrid g) {
        ForecastVO vo = new ForecastVO();
        vo.setId(g.getId());
        vo.setDataType(g.getVariable().toUpperCase());
        vo.setForecastDate(g.getForecastDate());
        vo.setLongitude(BigDecimal.valueOf(g.getLon()));
        vo.setLatitude(BigDecimal.valueOf(g.getLat()));
        vo.setValue(g.getValue());
        vo.setUnit(g.getUnit());
        return vo;
    }

    @Override
    public List<Map<String, Object>> getSstTrend(Double lon, Double lat) {
        return forecastGridMapper.selectTrend("sst", lon, lat);
    }

    @Override
    public List<Map<String, Object>> getChlTrend(Double lon, Double lat) {
        return forecastGridMapper.selectTrend("chl", lon, lat);
    }

    @Override
    public List<Map<String, Object>> getLocations() {
        return forecastGridMapper.selectDistinctLocations();
    }

    @Override
    public List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto) {
        return forecastGridMapper.selectMapGrid(
                dto.getDataType() != null ? dto.getDataType().toLowerCase() : "sst",
                dto.getForecastDate(),
                dto.getMinLon(), dto.getMaxLon(),
                dto.getMinLat(), dto.getMaxLat());
    }

    @Override
    public List<Map<String, Object>> getPointTrend(String dataType, Double lon, Double lat, String dateStart, String dateEnd) {
        return forecastGridMapper.selectPointTrend(dataType.toLowerCase(), lon, lat, dateStart, dateEnd);
    }

    @Override
    public List<Map<String, Object>> getDashboardTrend(String dataType, Integer days) {
        return forecastGridMapper.selectDashboardTrend(dataType.toLowerCase(), days);
    }

    @Override
    public List<Map<String, Object>> getChlProbability(String dateStart, String dateEnd, Double threshold) {
        return forecastGridMapper.selectChlProbability(dateStart, dateEnd, threshold != null ? threshold : 5.0);
    }

    @Override
    public List<Map<String, Object>> getSeaAreas() {
        List<Map<String, Object>> areas = new ArrayList<>();
        List<HealthZone> zones = healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1));
        for (HealthZone zone : zones) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", zone.getZoneName());
            m.put("minLon", zone.getMinLon());
            m.put("maxLon", zone.getMaxLon());
            m.put("minLat", zone.getMinLat());
            m.put("maxLat", zone.getMaxLat());
            areas.add(m);
        }
        return areas;
    }
}
