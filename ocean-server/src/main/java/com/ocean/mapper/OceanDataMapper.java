package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.OceanData;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 海洋观测数据 Mapper
 */
public interface OceanDataMapper extends BaseMapper<OceanData> {

    /**
     * 查询叶绿素浓度时间序列（按经纬度、日期范围聚合）
     */
    @Select("<script>" +
            "SELECT time, lat, lon, AVG(chl) AS chl FROM ocean_data " +
            "WHERE time BETWEEN #{startDate} AND #{endDate} " +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "GROUP BY time, lat, lon ORDER BY time ASC" +
            "</script>")
    List<Map<String, Object>> selectChlTimeSeries(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("lat") java.math.BigDecimal lat,
                                                   @Param("lon") java.math.BigDecimal lon);

    /**
     * 按深度维度的叶绿素浓度平均值
     */
    @Select("SELECT depth, AVG(chl) AS avg_chl, MIN(chl) AS min_chl, MAX(chl) AS max_chl " +
            "FROM ocean_data GROUP BY depth ORDER BY depth ASC")
    List<Map<String, Object>> selectChlByDepth();

    /**
     * 查询海表温度时间序列（按经纬度、日期范围聚合，排除 null）
     */
    @Select("<script>" +
            "SELECT time, lat, lon, AVG(sst) AS sst FROM ocean_data " +
            "WHERE time BETWEEN #{startDate} AND #{endDate} AND sst IS NOT NULL " +
            "<if test='lat != null'> AND lat = #{lat} </if>" +
            "<if test='lon != null'> AND lon = #{lon} </if>" +
            "GROUP BY time, lat, lon ORDER BY time ASC" +
            "</script>")
    List<Map<String, Object>> selectSstTimeSeries(@Param("startDate") String startDate,
                                                   @Param("endDate") String endDate,
                                                   @Param("lat") java.math.BigDecimal lat,
                                                   @Param("lon") java.math.BigDecimal lon);

    /**
     * 查询所有去重的经纬度
     */
    @Select("SELECT DISTINCT lat, lon FROM ocean_data ORDER BY lat, lon")
    List<Map<String, Object>> selectDistinctLonLat();
}
