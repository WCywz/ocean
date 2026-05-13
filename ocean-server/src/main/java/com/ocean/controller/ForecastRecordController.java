package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.dto.ZoneHealthQueryDTO;
import com.ocean.service.ForecastRecordService;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预报数据控制器（可视化数据接口）
 */
@RestController
@RequestMapping("/api/forecast")
public class ForecastRecordController {

    @Autowired
    private ForecastRecordService forecastRecordService;

    /**
     * 获取仪表盘数据
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        DashboardVO vo = forecastRecordService.getDashboard();
        return Result.success(vo);
    }

    /**
     * 分页查询预报数据
     */
    @GetMapping("/page")
    public Result<IPage<ForecastVO>> getRecordPage(@Validated ForecastQueryDTO dto) {
        IPage<ForecastVO> page = forecastRecordService.getRecordPage(dto);
        return Result.success(page);
    }

    /**
     * 获取海表温度趋势数据
     */
    @GetMapping("/sst/trend")
    public Result<List<Map<String, Object>>> getSstTrend(
            @RequestParam(required = false) BigDecimal lon,
            @RequestParam(required = false) BigDecimal lat) {
        List<Map<String, Object>> data = forecastRecordService.getSstTrend(lon, lat);
        return Result.success(data);
    }

    /**
     * 获取叶绿素浓度趋势数据
     */
    @GetMapping("/chl/trend")
    public Result<List<Map<String, Object>>> getChlTrend(
            @RequestParam(required = false) BigDecimal lon,
            @RequestParam(required = false) BigDecimal lat) {
        List<Map<String, Object>> data = forecastRecordService.getChlTrend(lon, lat);
        return Result.success(data);
    }

    /**
     * 获取所有去重的经纬度及观测点
     */
    @GetMapping("/locations")
    public Result<List<Map<String, Object>>> getDistinctLocations() {
        List<Map<String, Object>> data = forecastRecordService.getDistinctLocations();
        return Result.success(data);
    }

    /**
     * 获取地图网格聚合数据
     */
    @GetMapping("/map/grid")
    public Result<List<Map<String, Object>>> getMapGrid(@Validated MapGridQueryDTO dto) {
        List<Map<String, Object>> data = forecastRecordService.getMapGrid(dto);
        return Result.success(data);
    }

    /**
     * 获取单点位历史趋势
     */
    @GetMapping("/trend/point")
    public Result<List<Map<String, Object>>> getPointTrend(
            @RequestParam String dataType,
            @RequestParam BigDecimal lon,
            @RequestParam BigDecimal lat,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        List<Map<String, Object>> data = forecastRecordService.getPointTrend(dataType, lon, lat, dateStart, dateEnd);
        return Result.success(data);
    }

    /**
     * 仪表盘趋势数据
     */
    @GetMapping("/trend/dashboard")
    public Result<List<Map<String, Object>>> getDashboardTrend(
            @RequestParam(defaultValue = "SST") String dataType,
            @RequestParam(defaultValue = "7") Integer days) {
        List<Map<String, Object>> data = forecastRecordService.getDashboardTrend(dataType, days);
        return Result.success(data);
    }

    /**
     * 阈值告警
     */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> getAlerts(
            @RequestParam String forecastDate) {
        List<Map<String, Object>> data = forecastRecordService.getAlerts(forecastDate);
        return Result.success(data);
    }

    /**
     * 获取预设海域配置
     */
    @GetMapping("/sea-areas")
    public Result<List<Map<String, Object>>> getSeaAreas() {
        List<Map<String, Object>> data = forecastRecordService.getSeaAreas();
        return Result.success(data);
    }

    @GetMapping("/zone-health")
    public Result<Map<String, Object>> getZoneHealth(@Validated ZoneHealthQueryDTO dto) {
        Map<String, Object> data = forecastRecordService.getZoneHealth(dto);
        return Result.success(data);
    }
}
