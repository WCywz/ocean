package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.entity.ForecastRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 预报数据记录 Mapper
 */
public interface ForecastRecordMapper extends BaseMapper<ForecastRecord> {

    /**
     * 查询各观测点最新海表温度
     */
    @Select("SELECT fr.location_name AS locationName, fr.value, fr.unit, fr.forecast_date AS forecastDate " +
            "FROM forecast_record fr " +
            "INNER JOIN (SELECT location_name, MAX(forecast_date) AS max_date FROM forecast_record " +
            "WHERE data_type = 'SST' AND forecast_date = '2026-01-01' GROUP BY location_name) latest " +
            "ON fr.location_name = latest.location_name AND fr.forecast_date = latest.max_date " +
            "WHERE fr.data_type = 'SST' AND fr.forecast_date = '2026-01-01'")
    List<Map<String, Object>> selectLatestSstByLocation();

    /**
     * 查询各观测点最新叶绿素浓度
     */
    @Select("SELECT fr.location_name AS locationName, fr.value, fr.unit, fr.forecast_date AS forecastDate " +
            "FROM forecast_record fr " +
            "INNER JOIN (SELECT location_name, MAX(forecast_date) AS max_date FROM forecast_record " +
            "WHERE data_type = 'CHL' AND forecast_date = '2026-01-01' GROUP BY location_name) latest " +
            "ON fr.location_name = latest.location_name AND fr.forecast_date = latest.max_date " +
            "WHERE fr.data_type = 'CHL' AND fr.forecast_date = '2026-01-01'")
    List<Map<String, Object>> selectLatestChlByLocation();

    /**
     * 统计今日预报记录数
     */
    @Select("SELECT COUNT(*) FROM forecast_record WHERE forecast_date = '2026-01-01'")
    Long countTodayRecords();

    /**
     * 统计今日超出阈值的告警记录数 (SST>28°C 或 CHL>5 mg/m³)
     */
    @Select("SELECT COUNT(*) FROM forecast_record " +
            "WHERE forecast_date = '2026-01-01' " +
            "AND ((data_type = 'SST' AND value > 28) OR (data_type = 'CHL' AND value > 5))")
    Long countTodayAlerts();

    /**
     * 查询所有去重的经纬度及观测点名称
     */
    @Select("SELECT DISTINCT longitude, latitude, location_name AS locationName " +
            "FROM forecast_record " +
            "ORDER BY longitude, latitude")
    List<Map<String, Object>> selectDistinctLocations();

    /**
     * 原始点位查询 — SST / CHL 浓度模式（不做聚合，直接返回每条记录）
     */
    @Select("<script>" +
        "SELECT longitude, latitude, value " +
        "FROM forecast_record " +
        "WHERE data_type = #{dataType} " +
        "  AND forecast_date = #{forecastDate} " +
        "  <if test='minLon != null'> AND longitude &gt;= #{minLon} </if>" +
        "  <if test='maxLon != null'> AND longitude &lt;= #{maxLon} </if>" +
        "  <if test='minLat != null'> AND latitude &gt;= #{minLat} </if>" +
        "  <if test='maxLat != null'> AND latitude &lt;= #{maxLat} </if>" +
        "ORDER BY latitude, longitude" +
        "</script>")
    List<Map<String, Object>> selectRawPoints(MapGridQueryDTO dto);

    /**
     * 网格聚合查询 — Chl 概率模式
     */
    @Select("<script>" +
        "SELECT ROUND(longitude, #{precision}) AS gridLon, " +
        "       ROUND(latitude, #{precision}) AS gridLat, " +
        "       ROUND(SUM(CASE WHEN value &gt; #{threshold} THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 1) AS probability " +
        "FROM forecast_record " +
        "WHERE data_type = 'CHL' " +
        "  AND forecast_date BETWEEN #{dateStart} AND #{dateEnd} " +
        "  <if test='minLon != null'> AND longitude &gt;= #{minLon} </if>" +
        "  <if test='maxLon != null'> AND longitude &lt;= #{maxLon} </if>" +
        "  <if test='minLat != null'> AND latitude &gt;= #{minLat} </if>" +
        "  <if test='maxLat != null'> AND latitude &lt;= #{maxLat} </if>" +
        "GROUP BY gridLon, gridLat " +
        "ORDER BY gridLat, gridLon" +
        "</script>")
    List<Map<String, Object>> selectProbabilityGrid(MapGridQueryDTO dto);

    /**
     * 单点位趋势 — 查指定点位所有日期的值
     */
    @Select("<script>" +
        "SELECT forecast_date AS forecastDate, value " +
        "FROM forecast_record " +
        "WHERE data_type = #{dataType} " +
        "  AND longitude = #{lon} " +
        "  AND latitude = #{lat} " +
        "  <if test='dateStart != null and dateStart != \"\"'> AND forecast_date &gt;= #{dateStart} </if>" +
        "  <if test='dateEnd != null and dateEnd != \"\"'> AND forecast_date &lt;= #{dateEnd} </if>" +
        "ORDER BY forecast_date" +
        "</script>")
    List<Map<String, Object>> selectPointTrend(@Param("dataType") String dataType,
                                               @Param("lon") BigDecimal lon,
                                               @Param("lat") BigDecimal lat,
                                               @Param("dateStart") String dateStart,
                                               @Param("dateEnd") String dateEnd);

    /**
     * 仪表盘趋势 — 随机选 1 个观测点，查今天到未来 N 天的预测记录
     */
    @Select("SELECT fr.location_name AS locationName, " +
            "       fr.forecast_date AS forecastDate, " +
            "       fr.value, " +
            "       fr.longitude, " +
            "       fr.latitude " +
            "FROM forecast_record fr " +
            "JOIN ( " +
            "  SELECT location_name " +
            "  FROM forecast_record " +
            "  WHERE data_type = #{dataType} " +
            "    AND forecast_date >= '2026-01-01' " +
            "    AND forecast_date <= DATE_ADD('2026-01-01', INTERVAL #{days} DAY) " +
            "  GROUP BY location_name " +
            "  ORDER BY RAND() " +
            "  LIMIT 1 " +
            ") rand ON fr.location_name = rand.location_name " +
            "WHERE fr.data_type = #{dataType} " +
            "  AND fr.forecast_date >= '2026-01-01' " +
            "  AND fr.forecast_date <= DATE_ADD('2026-01-01', INTERVAL #{days} DAY) " +
            "ORDER BY fr.forecast_date")
    List<Map<String, Object>> selectDashboardTrend(@Param("dataType") String dataType,
                                                   @Param("days") Integer days);

    /**
     * 阈值告警详情 (SST>28°C 或 CHL>5 mg/m³)，按值降序，最多 20 条
     */
    @Select("SELECT location_name AS locationName, data_type AS dataType, " +
            "       value, forecast_date AS forecastDate, " +
            "       CASE WHEN data_type = 'SST' THEN 28 ELSE 5 END AS threshold " +
            "FROM forecast_record " +
            "WHERE forecast_date = #{forecastDate} " +
            "  AND ((data_type = 'SST' AND value > 28) OR (data_type = 'CHL' AND value > 5)) " +
            "ORDER BY value DESC " +
            "LIMIT 20")
    List<Map<String, Object>> selectAlertsByDate(@Param("forecastDate") String forecastDate);

    // ---- 分区健康指数 queries ----

    /**
     * 分区健康查询 — SST 统计（指定日期的均值、极值，以及相对历史均值的趋势方向）
     */
    @Select("<script>" +
        "SELECT " +
        "  AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) AS avgVal, " +
        "  MAX(CASE WHEN forecast_date = #{forecastDate} THEN value END) AS maxVal, " +
        "  CASE " +
        "    WHEN AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) &gt; " +
        "         AVG(CASE WHEN forecast_date &lt; #{forecastDate} THEN value END) THEN 'rising' " +
        "    WHEN AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) &lt; " +
        "         AVG(CASE WHEN forecast_date &lt; #{forecastDate} THEN value END) THEN 'falling' " +
        "    ELSE 'stable' END AS trend " +
        "FROM forecast_record " +
        "WHERE data_type = 'SST' " +
        "  AND forecast_date &lt;= #{forecastDate} " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "</script>")
    Map<String, Object> selectZoneSstStats(@Param("minLon") BigDecimal minLon,
                                           @Param("maxLon") BigDecimal maxLon,
                                           @Param("minLat") BigDecimal minLat,
                                           @Param("maxLat") BigDecimal maxLat,
                                           @Param("forecastDate") String forecastDate);

    /**
     * 分区健康查询 — Chl 统计（指定日期的均值、极值，以及相对历史均值的趋势方向）
     */
    @Select("<script>" +
        "SELECT " +
        "  AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) AS avgVal, " +
        "  MAX(CASE WHEN forecast_date = #{forecastDate} THEN value END) AS maxVal, " +
        "  CASE " +
        "    WHEN AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) &gt; " +
        "         AVG(CASE WHEN forecast_date &lt; #{forecastDate} THEN value END) THEN 'rising' " +
        "    WHEN AVG(CASE WHEN forecast_date = #{forecastDate} THEN value END) &lt; " +
        "         AVG(CASE WHEN forecast_date &lt; #{forecastDate} THEN value END) THEN 'falling' " +
        "    ELSE 'stable' END AS trend " +
        "FROM forecast_record " +
        "WHERE data_type = 'CHL' " +
        "  AND forecast_date &lt;= #{forecastDate} " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "</script>")
    Map<String, Object> selectZoneChlStats(@Param("minLon") BigDecimal minLon,
                                           @Param("maxLon") BigDecimal maxLon,
                                           @Param("minLat") BigDecimal minLat,
                                           @Param("maxLat") BigDecimal maxLat,
                                           @Param("forecastDate") String forecastDate);

    /**
     * 查询 SST 常年同期基准值（过去所有年份同月均值）
     */
    @Select("<script>" +
        "SELECT AVG(value) AS baseline " +
        "FROM forecast_record " +
        "WHERE data_type = 'SST' " +
        "  AND MONTH(forecast_date) = MONTH(#{forecastDate}) " +
        "  AND YEAR(forecast_date) &lt; YEAR(#{forecastDate}) " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "</script>")
    Map<String, Object> selectSstBaseline(@Param("minLon") BigDecimal minLon,
                                          @Param("maxLon") BigDecimal maxLon,
                                          @Param("minLat") BigDecimal minLat,
                                          @Param("maxLat") BigDecimal maxLat,
                                          @Param("forecastDate") String forecastDate);

    /**
     * 查询过去 30 天每日的区域 SST 均值（用于热浪检测）
     */
    @Select("<script>" +
        "SELECT forecast_date AS forecastDate, AVG(value) AS dailyAvg " +
        "FROM forecast_record " +
        "WHERE data_type = 'SST' " +
        "  AND forecast_date &lt;= #{forecastDate} " +
        "  AND forecast_date &gt;= DATE_SUB(#{forecastDate}, INTERVAL 30 DAY) " +
        "  AND longitude &gt;= #{minLon} AND longitude &lt;= #{maxLon} " +
        "  AND latitude &gt;= #{minLat} AND latitude &lt;= #{maxLat} " +
        "GROUP BY forecast_date " +
        "ORDER BY forecast_date DESC" +
        "</script>")
    List<Map<String, Object>> selectDailyAverages(@Param("minLon") BigDecimal minLon,
                                                  @Param("maxLon") BigDecimal maxLon,
                                                  @Param("minLat") BigDecimal minLat,
                                                  @Param("maxLat") BigDecimal maxLat,
                                                  @Param("forecastDate") String forecastDate);
}
