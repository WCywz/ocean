package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.ObservationData;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface ObservationDataMapper extends BaseMapper<ObservationData> {

    @Select("<script>" +
            "SELECT obs_time AS time, lat, lon, AVG(value) AS value FROM observation_data " +
            "WHERE variable = 'thetao' AND obs_time BETWEEN #{startDate} AND #{endDate} " +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "GROUP BY obs_time, lat, lon ORDER BY obs_time ASC" +
            "</script>")
    List<Map<String, Object>> selectSstTimeSeries(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("lat") Double lat,
                                                   @Param("lon") Double lon);

    @Select("<script>" +
            "SELECT obs_time AS time, lat, lon, AVG(value) AS value FROM observation_data " +
            "WHERE variable = 'chl' AND obs_time BETWEEN #{startDate} AND #{endDate} " +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "GROUP BY obs_time, lat, lon ORDER BY obs_time ASC" +
            "</script>")
    List<Map<String, Object>> selectChlTimeSeries(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("lat") Double lat,
                                                   @Param("lon") Double lon);

    @Select("SELECT depth, AVG(value) AS avg_value, MIN(value) AS min_value, MAX(value) AS max_value " +
            "FROM observation_data WHERE variable = 'chl' GROUP BY depth ORDER BY depth ASC")
    List<Map<String, Object>> selectChlByDepth();

    @Select("SELECT DISTINCT lat, lon FROM observation_grid_cache ORDER BY lat, lon")
    List<Map<String, Object>> selectDistinctLocations();

    @Select("SELECT obs_time AS time, depth, lat, lon, " +
            "MAX(CASE WHEN variable = 'chl' THEN value END) AS chl, " +
            "MAX(CASE WHEN variable = 'thetao' THEN value END) AS thetao, " +
            "MAX(CASE WHEN variable = 'so' THEN value END) AS so " +
            "FROM observation_data " +
            "WHERE obs_time BETWEEN #{startDate} AND #{endDate} " +
            "GROUP BY obs_time, depth, lat, lon " +
            "ORDER BY obs_time, lat, lon, depth")
    List<Map<String, Object>> selectForecastInput(@Param("startDate") String startDate,
                                                    @Param("endDate") String endDate);
}
