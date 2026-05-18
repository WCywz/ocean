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

    @Select("SELECT f.lat, f.lon, AVG(f.value) AS avg_value, MAX(f.forecast_date) AS forecastDate " +
            "FROM forecast_grid f WHERE f.variable = #{dataType} AND f.depth = 0 " +
            "AND f.forecast_date >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY f.lat, f.lon ORDER BY AVG(f.value) DESC LIMIT 5")
    List<Map<String, Object>> selectDashboardTrend(@Param("dataType") String dataType,
                                                    @Param("days") Integer days);

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
}
