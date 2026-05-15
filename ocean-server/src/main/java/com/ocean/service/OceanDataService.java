package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.vo.OceanDataVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 海洋观测数据服务接口
 */
public interface OceanDataService {

    /** 分页查询观测数据 */
    IPage<OceanDataVO> getDataPage(OceanDataQueryDTO dto);

    /** 获取海表温度时间序列 */
    List<Map<String, Object>> getSstTimeSeries(String startDate, String endDate,
                                                BigDecimal lat, BigDecimal lon);

    /** 获取叶绿素浓度时间序列 */
    List<Map<String, Object>> getChlTimeSeries(String startDate, String endDate,
                                                BigDecimal lat, BigDecimal lon);

    /** 获取按深度的叶绿素浓度分布 */
    List<Map<String, Object>> getChlByDepth();

    /** 获取所有去重的经纬度 */
    List<Map<String, Object>> getDistinctLonLat();
}
