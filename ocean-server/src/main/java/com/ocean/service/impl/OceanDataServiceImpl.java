package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.entity.OceanData;
import com.ocean.mapper.OceanDataMapper;
import com.ocean.service.OceanDataService;
import com.ocean.vo.OceanDataVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 海洋观测数据服务实现
 */
@Service
public class OceanDataServiceImpl implements OceanDataService {

    @Autowired
    private OceanDataMapper oceanDataMapper;

    @Override
    public IPage<OceanDataVO> getDataPage(OceanDataQueryDTO dto) {
        Page<OceanData> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<OceanData> wrapper = new LambdaQueryWrapper<>();

        if (dto.getLon() != null && !dto.getLon().isEmpty()) {
            wrapper.eq(OceanData::getLon, new BigDecimal(dto.getLon()));
        }
        if (dto.getLat() != null && !dto.getLat().isEmpty()) {
            wrapper.eq(OceanData::getLat, new BigDecimal(dto.getLat()));
        }
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            wrapper.ge(OceanData::getTime, dto.getStartDate());
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            wrapper.le(OceanData::getTime, dto.getEndDate());
        }
        if (dto.getDepthMin() != null) {
            wrapper.ge(OceanData::getDepth, dto.getDepthMin());
        }
        if (dto.getDepthMax() != null) {
            wrapper.le(OceanData::getDepth, dto.getDepthMax());
        }

        wrapper.orderByDesc(OceanData::getTime)
               .orderByAsc(OceanData::getDepth);

        IPage<OceanData> dataPage = oceanDataMapper.selectPage(page, wrapper);
        return dataPage.convert(this::toVO);
    }

    @Override
    public List<Map<String, Object>> getSstTimeSeries(String startDate, String endDate,
                                                       BigDecimal lat, BigDecimal lon) {
        return oceanDataMapper.selectSstTimeSeries(startDate, endDate, lat, lon);
    }

    @Override
    public List<Map<String, Object>> getChlTimeSeries(String startDate, String endDate,
                                                       BigDecimal lat, BigDecimal lon) {
        return oceanDataMapper.selectChlTimeSeries(startDate, endDate, lat, lon);
    }

    @Override
    public List<Map<String, Object>> getChlByDepth() {
        return oceanDataMapper.selectChlByDepth();
    }

    @Override
    public List<Map<String, Object>> getDistinctLonLat() {
        return oceanDataMapper.selectDistinctLonLat();
    }

    private OceanDataVO toVO(OceanData data) {
        OceanDataVO vo = new OceanDataVO();
        BeanUtils.copyProperties(data, vo);
        return vo;
    }
}
