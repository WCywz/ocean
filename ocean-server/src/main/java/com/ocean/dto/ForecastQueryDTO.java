package com.ocean.dto;

import lombok.Data;

/**
 * 预报数据查询参数
 */
@Data
public class ForecastQueryDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /** 数据类型: SST / CHL */
    private String dataType;

    /** 观测点名称 */
    private String locationName;

    /** 预报日期开始 */
    private String forecastDateBegin;

    /** 预报日期结束 */
    private String forecastDateEnd;
}
