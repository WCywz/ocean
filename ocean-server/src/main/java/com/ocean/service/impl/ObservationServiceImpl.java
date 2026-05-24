package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.entity.ObservationData;
import com.ocean.mapper.ObservationDataMapper;
import com.ocean.service.ObservationService;
import com.ocean.vo.OceanDataVO;
import com.ocean.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ObservationServiceImpl implements ObservationService {

    @Autowired
    private ObservationDataMapper observationDataMapper;
    @Autowired
    private SystemConfigService systemConfigService;

    @Override
    public IPage<OceanDataVO> getDataPage(OceanDataQueryDTO dto) {
        Page<ObservationData> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ObservationData> wrapper = new LambdaQueryWrapper<>();
        if (dto.getVariable() != null && !dto.getVariable().isEmpty()) {
            wrapper.eq(ObservationData::getVariable, dto.getVariable());
        }
        if (dto.getLon() != null && !dto.getLon().isEmpty()) {
            wrapper.eq(ObservationData::getLon, Double.parseDouble(dto.getLon()));
        }
        if (dto.getLat() != null && !dto.getLat().isEmpty()) {
            wrapper.eq(ObservationData::getLat, Double.parseDouble(dto.getLat()));
        }
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            wrapper.ge(ObservationData::getObsTime, LocalDate.parse(dto.getStartDate()));
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            wrapper.le(ObservationData::getObsTime, LocalDate.parse(dto.getEndDate()));
        }
        // Default: last 30 days of data to avoid expensive COUNT on 34M+ rows
        if ((dto.getStartDate() == null || dto.getStartDate().isEmpty())
                && (dto.getEndDate() == null || dto.getEndDate().isEmpty())) {
            wrapper.ge(ObservationData::getObsTime, systemConfigService.getSystemDate().minusDays(30));
        }
        if (dto.getDepthMin() != null) wrapper.ge(ObservationData::getDepth, dto.getDepthMin());
        if (dto.getDepthMax() != null) wrapper.le(ObservationData::getDepth, dto.getDepthMax());
        wrapper.orderByDesc(ObservationData::getObsTime).orderByAsc(ObservationData::getDepth);
        IPage<ObservationData> dataPage = observationDataMapper.selectPage(page, wrapper);
        return dataPage.convert(this::toVO);
    }

    @Override
    public List<Map<String, Object>> getSstTimeSeries(String startDate, String endDate, Double lat, Double lon) {
        return observationDataMapper.selectSstTimeSeries(startDate, endDate, lat, lon);
    }

    @Override
    public List<Map<String, Object>> getChlTimeSeries(String startDate, String endDate, Double lat, Double lon) {
        return observationDataMapper.selectChlTimeSeries(startDate, endDate, lat, lon);
    }

    @Override
    public List<Map<String, Object>> getChlByDepth() {
        return observationDataMapper.selectChlByDepth();
    }

    @Override
    public List<Map<String, Object>> getDistinctLocations() {
        return observationDataMapper.selectDistinctLocations();
    }

    private OceanDataVO toVO(ObservationData data) {
        OceanDataVO vo = new OceanDataVO();
        vo.setId(data.getId());
        vo.setVariable(data.getVariable());
        vo.setObsDate(data.getObsTime());
        vo.setValue(data.getValue());
        vo.setUnit(unitOf(data.getVariable()));
        vo.setDepth(data.getDepth());
        vo.setLat(data.getLat());
        vo.setLon(data.getLon());
        vo.setCreateTime(data.getCreateTime());
        return vo;
    }

    private static String unitOf(String variable) {
        if (variable == null) return null;
        switch (variable) {
            case "thetao": return "degree_C";
            case "chl": return "mg_m3";
            case "so": return "psu";
            default: return null;
        }
    }
}
