package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预报数据服务接口
 */
public interface ForecastRecordService {

    /** 分页查询预报数据 */
    IPage<ForecastVO> getRecordPage(ForecastQueryDTO dto);

    /** 获取仪表盘数据 */
    DashboardVO getDashboard();

    /** 仪表盘趋势数据 — top N 观测点最近天数日均值 */
    List<Map<String, Object>> getDashboardTrend(String dataType, Integer days);

    /** 今日阈值告警详情 */
    List<Map<String, Object>> getTodayAlerts();

    /** 获取各观测点SST历史趋势数据 */
    List<Map<String, Object>> getSstTrend(BigDecimal lon, BigDecimal lat);

    /** 获取各观测点CHL历史趋势数据 */
    List<Map<String, Object>> getChlTrend(BigDecimal lon, BigDecimal lat);

    /** 获取所有去重的经纬度及观测点 */
    List<Map<String, Object>> getDistinctLocations();

    /** 地图网格聚合数据 */
    List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto);

    /** 单点位历史趋势 */
    List<Map<String, Object>> getPointTrend(String dataType, BigDecimal lon, BigDecimal lat, String dateStart, String dateEnd);

    /** 预设海域配置 */
    List<Map<String, Object>> getSeaAreas();
}
