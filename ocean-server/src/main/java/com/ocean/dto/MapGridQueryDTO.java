package com.ocean.dto;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 地图网格聚合查询参数
 */
@Data
public class MapGridQueryDTO {

    /** 数据类型: SST / CHL */
    private String dataType;

    /** 预报日期 */
    private String forecastDate;

    /** 日期范围开始 (Chl 概率模式) */
    private String dateStart;

    /** 日期范围结束 (Chl 概率模式) */
    private String dateEnd;

    /** 聚合精度（度），默认 0.05 */
    private Double precision;

    /** 可视范围西边界 */
    private BigDecimal minLon;

    /** 可视范围东边界 */
    private BigDecimal maxLon;

    /** 可视范围南边界 */
    private BigDecimal minLat;

    /** 可视范围北边界 */
    private BigDecimal maxLat;

    /** Chl 渲染模式: concentration / probability */
    private String chlMode;

    /** 概率模式超阈值，默认 3.0 */
    private Double threshold;
}
