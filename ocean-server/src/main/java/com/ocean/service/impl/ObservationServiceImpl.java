package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.entity.ObservationData;
import com.ocean.mapper.ObservationDataMapper;
import com.ocean.service.ObservationService;
import com.ocean.vo.OceanDataVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class ObservationServiceImpl implements ObservationService {

    @Autowired
    private ObservationDataMapper observationDataMapper;

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
        BeanUtils.copyProperties(data, vo);
        return vo;
    }
}
