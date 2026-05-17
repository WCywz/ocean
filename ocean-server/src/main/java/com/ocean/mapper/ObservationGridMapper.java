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
}
