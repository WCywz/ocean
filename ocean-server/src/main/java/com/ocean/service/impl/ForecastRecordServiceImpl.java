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
import com.ocean.dto.ZoneHealthQueryDTO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        // 今日告警数
        vo.setAlertCount(forecastRecordMapper.countTodayAlerts());
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

    @Override
    public List<Map<String, Object>> getDashboardTrend(String dataType, Integer days) {
        if (days == null) days = 7;
        List<Map<String, Object>> rows = forecastRecordMapper.selectDashboardTrend(dataType, days);
        // Group by locationName, build {locationName, dataPoints: [{date, value}]}
        Map<String, List<Map<String, Object>>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row.get("locationName"),
                        Collectors.toList()
                ));
        return grouped.entrySet().stream().map(entry -> {
            Map<String, Object> m = new HashMap<>();
            m.put("locationName", entry.getKey());
            m.put("dataPoints", entry.getValue().stream().map(r -> {
                Map<String, Object> dp = new HashMap<>();
                dp.put("date", r.get("forecastDate").toString());
                dp.put("value", r.get("value"));
                return dp;
            }).collect(Collectors.toList()));
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getTodayAlerts() {
        return forecastRecordMapper.selectTodayAlerts();
    }

    private static final BigDecimal ONE_DEG = new BigDecimal("1.0");
    private static final BigDecimal OFFSHORE_WIDTH = new BigDecimal("5.0");
    private static final BigDecimal LAT_EXTENT = new BigDecimal("3.0");
    private static final double HEATWAVE_THRESHOLD = 2.0;
    private static final int HEATWAVE_MIN_DAYS = 5;

    @Override
    public Map<String, Object> getZoneHealth(ZoneHealthQueryDTO dto) {
        BigDecimal centerLon = requireNonNull(dto.getCenterLon(), "centerLon");
        BigDecimal centerLat = requireNonNull(dto.getCenterLat(), "centerLat");
        BigDecimal coastLon = requireNonNull(dto.getCoastLon(), "coastLon");
        String forecastDate = dto.getForecastDate();
        if (forecastDate == null || forecastDate.isEmpty()) {
            throw new IllegalArgumentException("forecastDate is required");
        }

        BigDecimal[][] zoneDefs = {
            {coastLon, centerLon,                     centerLat,         centerLat.add(LAT_EXTENT)},
            {coastLon, centerLon,                     centerLat.subtract(LAT_EXTENT), centerLat},
            {centerLon, centerLon.add(ONE_DEG),       centerLat,         centerLat.add(LAT_EXTENT)},
            {centerLon, centerLon.add(ONE_DEG),       centerLat.subtract(LAT_EXTENT), centerLat},
            {centerLon.add(ONE_DEG), centerLon.add(OFFSHORE_WIDTH), centerLat, centerLat.add(LAT_EXTENT)},
            {centerLon.add(ONE_DEG), centerLon.add(OFFSHORE_WIDTH), centerLat.subtract(LAT_EXTENT), centerLat}
        };

        String[][] zoneLabels = {
            {"nearshore-north", "近岸北"}, {"nearshore-south", "近岸南"},
            {"transition-north", "过渡带北"}, {"transition-south", "过渡带南"},
            {"offshore-north", "远海北"}, {"offshore-south", "远海南"}
        };

        List<Map<String, Object>> zones = new ArrayList<>();
        for (int i = 0; i < zoneDefs.length; i++) {
            BigDecimal[] def = zoneDefs[i];
            BigDecimal minLon = def[0], maxLon = def[1], minLat = def[2], maxLat = def[3];

            Map<String, Object> sstStats = forecastRecordMapper.selectZoneSstStats(
                minLon, maxLon, minLat, maxLat, forecastDate);
            Map<String, Object> chlStats = forecastRecordMapper.selectZoneChlStats(
                minLon, maxLon, minLat, maxLat, forecastDate);
            Map<String, Object> baseline = forecastRecordMapper.selectSstBaseline(
                minLon, maxLon, minLat, maxLat, forecastDate);

            Double sstAvg = toDouble(sstStats != null ? sstStats.get("avgVal") : null);
            Double sstMax = toDouble(sstStats != null ? sstStats.get("maxVal") : null);
            String sstTrend = sstStats != null ? (String) sstStats.getOrDefault("trend", "stable") : "stable";
            Double sstBaseline = toDouble(baseline != null ? baseline.get("baseline") : null);
            Double anomaly = (sstAvg != null && sstBaseline != null) ? sstAvg - sstBaseline : 0.0;

            boolean heatActive = false;
            int heatDays = 0;
            if (sstBaseline != null) {
                List<Map<String, Object>> dailyAvgs = forecastRecordMapper.selectDailyAverages(
                    minLon, maxLon, minLat, maxLat, forecastDate);
                double threshold = sstBaseline + HEATWAVE_THRESHOLD;
                int maxStreak = 0, currentStreak = 0;
                for (Map<String, Object> day : dailyAvgs) {
                    double dailyAvg = toDouble(day.get("dailyAvg"));
                    if (dailyAvg > threshold) { currentStreak++; if (currentStreak > maxStreak) maxStreak = currentStreak; }
                    else currentStreak = 0;
                }
                heatDays = maxStreak;
                heatActive = maxStreak >= HEATWAVE_MIN_DAYS;
            }

            Double chlAvg = toDouble(chlStats != null ? chlStats.get("avgVal") : null);
            Double chlMax = toDouble(chlStats != null ? chlStats.get("maxVal") : null);
            String chlTrend = chlStats != null ? (String) chlStats.getOrDefault("trend", "stable") : "stable";

            Map<String, Object> sstMap = new LinkedHashMap<>();
            sstMap.put("avg", sstAvg != null ? sstAvg : 0);
            sstMap.put("max", sstMax != null ? sstMax : 0);
            sstMap.put("trend", sstTrend);
            sstMap.put("anomaly", anomaly);

            Map<String, Object> chlMap = new LinkedHashMap<>();
            chlMap.put("avg", chlAvg != null ? chlAvg : 0);
            chlMap.put("max", chlMax != null ? chlMax : 0);
            chlMap.put("trend", chlTrend);

            Map<String, Object> hwMap = new LinkedHashMap<>();
            hwMap.put("active", heatActive);
            hwMap.put("days", heatDays);

            Map<String, Object> zone = new LinkedHashMap<>();
            zone.put("id", zoneLabels[i][0]);
            zone.put("label", zoneLabels[i][1]);
            zone.put("sst", sstMap);
            zone.put("chl", chlMap);
            zone.put("heatwave", hwMap);
            zones.add(zone);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("zoneName", "东海");
        result.put("zones", zones);
        return result;
    }

    private static BigDecimal requireNonNull(BigDecimal value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        return ((Number) val).doubleValue();
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
