package com.ocean.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 首页仪表盘数据
 */
@Data
public class DashboardVO {

    /** 模型总数 */
    private Long modelCount;

    /** 运行中模型数 */
    private Long runningModelCount;

    /** 今日预报记录数 */
    private Long todayRecordCount;

    /** 今日告警数 */
    private Long alertCount;

    /** 各观测点最新SST数据 */
    private List<Map<String, Object>> latestSstData;

    /** 各观测点最新CHL数据 */
    private List<Map<String, Object>> latestChlData;
}
