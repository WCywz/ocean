package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.ForecastGrid;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ForecastGridMapper extends BaseMapper<ForecastGrid> {

    @Select("SELECT DISTINCT lat, lon FROM forecast_grid ORDER BY lat, lon")
    List<Map<String, Object>> selectDistinctLocations();

    @Select("<script>" +
            "SELECT lat AS gridLat, lon AS gridLon, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{variable} AND forecast_date = #{forecastDate} AND depth = 0 " +
            "<if test='minLon != null'> AND lon &gt;= #{minLon} </if>" +
            "<if test='maxLon != null'> AND lon &lt;= #{maxLon} </if>" +
            "<if test='minLat != null'> AND lat &gt;= #{minLat} </if>" +
            "<if test='maxLat != null'> AND lat &lt;= #{maxLat} </if>" +
            "ORDER BY lat, lon" +
            "</script>")
    List<Map<String, Object>> selectMapGrid(@Param("variable") String variable,
                                             @Param("forecastDate") String forecastDate,
                                             @Param("minLon") Double minLon,
                                             @Param("maxLon") Double maxLon,
                                             @Param("minLat") Double minLat,
                                             @Param("maxLat") Double maxLat);

    @Select("<script>" +
            "SELECT forecast_date AS forecastDate, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{dataType} AND depth = 0 " +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "ORDER BY forecast_date ASC" +
            "</script>")
    List<Map<String, Object>> selectTrend(@Param("dataType") String dataType,
                                           @Param("lon") Double lon,
                                           @Param("lat") Double lat);

    @Select("<script>" +
            "SELECT forecast_date AS forecastDate, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{dataType} AND depth = 0 " +
            "AND lat = #{lat} AND lon = #{lon} " +
            "<if test='dateStart != null'> AND forecast_date &gt;= #{dateStart} </if>" +
            "<if test='dateEnd != null'> AND forecast_date &lt;= #{dateEnd} </if>" +
            "ORDER BY forecast_date ASC" +
            "</script>")
    List<Map<String, Object>> selectPointTrend(@Param("dataType") String dataType,
                                                @Param("lon") Double lon,
                                                @Param("lat") Double lat,
                                                @Param("dateStart") String dateStart,
                                                @Param("dateEnd") String dateEnd);

    @Select("SELECT forecast_date AS date, value " +
            "FROM forecast_grid WHERE variable = #{dataType} AND depth = 0 " +
            "AND lat = #{lat} AND lon = #{lon} " +
            "AND forecast_date >= #{startDate} AND forecast_date < #{endDate} " +
            "ORDER BY forecast_date")
    List<Map<String, Object>> selectDashboardTrend(@Param("dataType") String dataType,
                                                    @Param("lat") Double lat,
                                                    @Param("lon") Double lon,
                                                    @Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);

    @Select("SELECT lat, lon, " +
            "POW(lat - #{centerLat}, 2) + POW(lon - #{centerLon}, 2) AS dist " +
            "FROM forecast_grid WHERE depth = 0 AND forecast_date >= #{fromDate} " +
            "ORDER BY dist LIMIT 1")
    Map<String, Object> selectNearestPoint(@Param("centerLat") Double centerLat,
                                           @Param("centerLon") Double centerLon,
                                           @Param("fromDate") String fromDate);

    @Select("SELECT lat, lon, " +
            "SUM(CASE WHEN value > #{threshold} THEN 1 ELSE 0 END) / COUNT(*) AS probability " +
            "FROM forecast_grid " +
            "WHERE variable = 'chl' AND depth = 0 " +
            "AND forecast_date BETWEEN #{dateStart} AND #{dateEnd} " +
            "GROUP BY lat, lon")
    List<Map<String, Object>> selectChlProbability(@Param("dateStart") String dateStart,
                                                    @Param("dateEnd") String dateEnd,
                                                    @Param("threshold") Double threshold);

    @Select("<script>" +
            "SELECT lat, lon, value " +
            "FROM forecast_grid " +
            "WHERE variable = #{variable} AND forecast_date = #{forecastDate} AND depth = #{depth} " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon}" +
            "</script>")
    List<Map<String, Object>> selectByBbox(@Param("variable") String variable,
                                            @Param("forecastDate") String forecastDate,
                                            @Param("depth") Double depth,
                                            @Param("minLon") Double minLon,
                                            @Param("maxLon") Double maxLon,
                                            @Param("minLat") Double minLat,
                                            @Param("maxLat") Double maxLat);

    @Select("SELECT AVG(value) AS avg_val, MAX(value) AS max_val " +
            "FROM forecast_grid " +
            "WHERE variable = #{variable} AND forecast_date = #{forecastDate} AND depth = 0 " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon}")
    Map<String, Object> selectZoneStats(@Param("variable") String variable,
                                         @Param("forecastDate") String forecastDate,
                                         @Param("minLon") Double minLon,
                                         @Param("maxLon") Double maxLon,
                                         @Param("minLat") Double minLat,
                                         @Param("maxLat") Double maxLat);

    @Select("SELECT forecast_date AS date, AVG(value) AS avg_val " +
            "FROM forecast_grid " +
            "WHERE variable = 'sst' AND depth = 0 " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon} " +
            "AND forecast_date <= #{endDate} AND forecast_date >= #{startDate} " +
            "GROUP BY forecast_date ORDER BY forecast_date ASC")
    List<Map<String, Object>> selectZoneDailyAvg(@Param("minLon") Double minLon,
                                                   @Param("maxLon") Double maxLon,
                                                   @Param("minLat") Double minLat,
                                                   @Param("maxLat") Double maxLat,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);

    @Select("SELECT AVG(value) AS avg_val " +
            "FROM forecast_grid " +
            "WHERE variable = 'sst' AND depth = 0 " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon}")
    Double selectZoneSstBaseline(@Param("minLon") Double minLon,
                                  @Param("maxLon") Double maxLon,
                                  @Param("minLat") Double minLat,
                                  @Param("maxLat") Double maxLat);
}
