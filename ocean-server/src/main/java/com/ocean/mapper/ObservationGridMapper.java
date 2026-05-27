package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.ObservationGrid;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ObservationGridMapper extends BaseMapper<ObservationGrid> {

    @Select("SELECT DISTINCT lat, lon FROM observation_grid ORDER BY lat, lon")
    List<Map<String, Object>> selectDistinctLocations();

    @Select("<script>" +
            "SELECT lat AS gridLat, lon AS gridLon, value " +
            "FROM observation_grid " +
            "WHERE variable = #{variable} AND obs_date = #{obsDate} AND depth = 0 " +
            "<if test='minLon != null'> AND lon &gt;= #{minLon} </if>" +
            "<if test='maxLon != null'> AND lon &lt;= #{maxLon} </if>" +
            "<if test='minLat != null'> AND lat &gt;= #{minLat} </if>" +
            "<if test='maxLat != null'> AND lat &lt;= #{maxLat} </if>" +
            "ORDER BY lat, lon" +
            "</script>")
    List<Map<String, Object>> selectMapGrid(@Param("variable") String variable,
                                             @Param("obsDate") String obsDate,
                                             @Param("minLon") Double minLon,
                                             @Param("maxLon") Double maxLon,
                                             @Param("minLat") Double minLat,
                                             @Param("maxLat") Double maxLat);

    @Select("<script>" +
            "SELECT obs_date AS obsDate, value " +
            "FROM observation_grid " +
            "WHERE variable = #{dataType} AND depth = 0 " +
            "AND lat = #{lat} AND lon = #{lon} " +
            "<if test='dateStart != null'> AND obs_date &gt;= #{dateStart} </if>" +
            "<if test='dateEnd != null'> AND obs_date &lt;= #{dateEnd} </if>" +
            "ORDER BY obs_date ASC" +
            "</script>")
    List<Map<String, Object>> selectPointTrend(@Param("dataType") String dataType,
                                                @Param("lon") Double lon,
                                                @Param("lat") Double lat,
                                                @Param("dateStart") String dateStart,
                                                @Param("dateEnd") String dateEnd);

    @Select("<script>" +
            "SELECT obs_date AS obsDate, value " +
            "FROM observation_grid " +
            "WHERE variable = #{dataType} AND depth = 0 " +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "ORDER BY obs_date ASC" +
            "</script>")
    List<Map<String, Object>> selectTrend(@Param("dataType") String dataType,
                                           @Param("lon") Double lon,
                                           @Param("lat") Double lat);

    @Select("SELECT AVG(value) AS avg_val, MAX(value) AS max_val " +
            "FROM observation_grid " +
            "WHERE variable = #{variable} AND obs_date = #{obsDate} AND depth = 0 " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon}")
    Map<String, Object> selectZoneStats(@Param("variable") String variable,
                                         @Param("obsDate") String obsDate,
                                         @Param("minLon") Double minLon,
                                         @Param("maxLon") Double maxLon,
                                         @Param("minLat") Double minLat,
                                         @Param("maxLat") Double maxLat);

    @Select("SELECT AVG(value) AS avg_val " +
            "FROM observation_grid " +
            "WHERE variable = #{variable} AND depth = 0 " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon} " +
            "AND obs_date <= #{endDate} AND obs_date >= #{startDate} " +
            "GROUP BY obs_date ORDER BY obs_date ASC")
    List<Map<String, Object>> selectZoneDailyAvg(@Param("variable") String variable,
                                                  @Param("minLon") Double minLon,
                                                  @Param("maxLon") Double maxLon,
                                                  @Param("minLat") Double minLat,
                                                  @Param("maxLat") Double maxLat,
                                                  @Param("startDate") String startDate,
                                                  @Param("endDate") String endDate);

    @Select("SELECT AVG(value) AS avg_val " +
            "FROM observation_grid " +
            "WHERE variable = #{variable} AND depth = 0 " +
            "AND MONTH(obs_date) = #{month} " +
            "AND lat BETWEEN #{minLat} AND #{maxLat} " +
            "AND lon BETWEEN #{minLon} AND #{maxLon}")
    Double selectZoneBaseline(@Param("variable") String variable,
                               @Param("month") int month,
                               @Param("minLon") Double minLon,
                               @Param("maxLon") Double maxLon,
                               @Param("minLat") Double minLat,
                               @Param("maxLat") Double maxLat);

    @Select("SELECT og.lat, og.lon, og.value, " +
            "(og.value - COALESCE(b.avg_val, og.value)) AS anomaly " +
            "FROM observation_grid og " +
            "LEFT JOIN (SELECT lat, lon, AVG(value) AS avg_val " +
            "           FROM observation_grid " +
            "           WHERE variable = 'thetao' AND depth = 0 AND MONTH(obs_date) = #{month} " +
            "           AND lat BETWEEN 20 AND 35 AND lon BETWEEN 118 AND 130 " +
            "           AND obs_date >= '2021-01-01' " +
            "           GROUP BY lat, lon) b ON og.lat = b.lat AND og.lon = b.lon " +
            "WHERE og.variable = 'thetao' AND og.depth = 0 AND og.obs_date = #{obsDate} " +
            "AND og.lon >= 121.83 " +
            "AND ABS(og.value - COALESCE(b.avg_val, og.value)) > #{minAnomaly} " +
            "ORDER BY ABS(og.value - COALESCE(b.avg_val, og.value)) DESC")
    List<Map<String, Object>> selectHotspots(@Param("obsDate") String obsDate,
                                              @Param("month") int month,
                                              @Param("minAnomaly") double minAnomaly);
}
