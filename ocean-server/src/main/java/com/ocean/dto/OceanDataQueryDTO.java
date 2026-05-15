package com.ocean.dto;

import lombok.Data;

/**
 * 海洋观测数据查询参数
 */
@Data
public class OceanDataQueryDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /** 纬度 */
    private String lat;

    /** 经度 */
    private String lon;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;

    /** 深度最小值 */
    private Double depthMin;

    /** 深度最大值 */
    private Double depthMax;
}
