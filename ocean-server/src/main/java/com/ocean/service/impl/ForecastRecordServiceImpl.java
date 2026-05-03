package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.config.SeaAreaConfig;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.entity.ForecastModel;
import com.ocean.entity.ForecastRecord;
import com.ocean.mapper.ForecastModelMapper;
import com.ocean.mapper.ForecastRecordMapper;
import com.ocean.service.ForecastRecordService;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预报数据服务实现
 */
@Service
public class ForecastRecordServiceImpl implements ForecastRecordService {

    @Autowired
    private ForecastRecordMapper forecastRecordMapper;

    @Autowired
    private ForecastModelMapper forecastModelMapper;

    @Autowired
    private SeaAreaConfig seaAreaConfig;

    @Override
    public IPage<ForecastVO> getRecordPage(ForecastQueryDTO dto) {
        Page<ForecastRecord> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ForecastRecord> wrapper = new LambdaQueryWrapper<>();

        if (dto.getDataType() != null && !dto.getDataType().isEmpty()) {
            wrapper.eq(ForecastRecord::getDataType, dto.getDataType());
        }
        if (dto.getLocationName() != null && !dto.getLocationName().isEmpty()) {
            wrapper.eq(ForecastRecord::getLocationName, dto.getLocationName());
        }
        if (dto.getForecastDateBegin() != null && !dto.getForecastDateBegin().isEmpty()) {
            wrapper.ge(ForecastRecord::getForecastDate, dto.getForecastDateBegin());
        }
        if (dto.getForecastDateEnd() != null && !dto.getForecastDateEnd().isEmpty()) {
            wrapper.le(ForecastRecord::getForecastDate, dto.getForecastDateEnd());
        }
        wrapper.orderByDesc(ForecastRecord::getForecastDate)
               .orderByAsc(ForecastRecord::getLocationName);

        IPage<ForecastRecord> recordPage = forecastRecordMapper.selectPage(page, wrapper);
        return recordPage.convert(this::toVO);
    }

    @Override
    @Cacheable(value = "dashboard", key = "'dashboard'", unless = "#result == null")
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        // 模型总数
        vo.setModelCount(forecastModelMapper.selectCount(null));
        // 运行中模型数
        vo.setRunningModelCount(forecastModelMapper.selectCount(
                new LambdaQueryWrapper<ForecastModel>().eq(ForecastModel::getStatus, "RUNNING")
        ));
        // 今日预报记录数
        vo.setTodayRecordCount(forecastRecordMapper.countTodayRecords());
        // 最新SST和CHL数据
        vo.setLatestSstData(forecastRecordMapper.selectLatestSstByLocation());
        vo.setLatestChlData(forecastRecordMapper.selectLatestChlByLocation());
        return vo;
    }

    @Override
    public List<Map<String, Object>> getSstTrend(BigDecimal lon, BigDecimal lat) {
        return forecastRecordMapper.selectList(
                new LambdaQueryWrapper<ForecastRecord>()
                        .eq(ForecastRecord::getDataType, "SST")
                        .eq(lon != null, ForecastRecord::getLongitude, lon)
                        .eq(lat != null, ForecastRecord::getLatitude, lat)
                        .orderByAsc(ForecastRecord::getForecastDate)
        ).stream().map(r -> Map.<String, Object>of(
                "forecastDate", r.getForecastDate().toString(),
                "longitude", r.getLongitude(),
                "latitude", r.getLatitude(),
                "locationName", r.getLocationName(),
                "value", r.getValue()
        )).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getChlTrend(BigDecimal lon, BigDecimal lat) {
        return forecastRecordMapper.selectList(
                new LambdaQueryWrapper<ForecastRecord>()
                        .eq(ForecastRecord::getDataType, "CHL")
                        .eq(lon != null, ForecastRecord::getLongitude, lon)
                        .eq(lat != null, ForecastRecord::getLatitude, lat)
                        .orderByAsc(ForecastRecord::getForecastDate)
        ).stream().map(r -> Map.<String, Object>of(
                "forecastDate", r.getForecastDate().toString(),
                "longitude", r.getLongitude(),
                "latitude", r.getLatitude(),
                "locationName", r.getLocationName(),
                "value", r.getValue()
        )).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getDistinctLocations() {
        return forecastRecordMapper.selectDistinctLocations();
    }

    @Override
    public List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto) {
        if (dto.getPrecision() == null) {
            dto.setPrecision(0.05);
        }
        if ("probability".equals(dto.getChlMode())) {
            if (dto.getThreshold() == null) {
                dto.setThreshold(3.0);
            }
            return forecastRecordMapper.selectProbabilityGrid(dto);
        }
        return forecastRecordMapper.selectAggregatedGrid(dto);
    }

    @Override
    public List<Map<String, Object>> getPointTrend(String dataType, BigDecimal lon, BigDecimal lat,
                                                    String dateStart, String dateEnd) {
        return forecastRecordMapper.selectPointTrend(dataType, lon, lat, dateStart, dateEnd);
    }

    @Override
    public List<Map<String, Object>> getSeaAreas() {
        return seaAreaConfig.getSeaAreas();
    }

    private ForecastVO toVO(ForecastRecord record) {
        ForecastVO vo = new ForecastVO();
        BeanUtils.copyProperties(record, vo);
        // 关联查询模型名称
        ForecastModel model = forecastModelMapper.selectById(record.getModelId());
        if (model != null) {
            vo.setModelName(model.getModelName());
        }
        return vo;
    }


}
