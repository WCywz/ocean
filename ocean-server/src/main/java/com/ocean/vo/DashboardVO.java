package com.ocean.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 仪表盘视图对象
 */
@Data
public class DashboardVO {

    /** 模型总数 */
    private Long modelCount;

    /** 运行中模型数 */
    private Long runningModelCount;

    /** 今日预报记录数 */
    private Long todayRecordCount;

    /** 今日超出阈值的告警记录数 */
    private Long alertCount;

    /** 最新SST观测点数据 */
    private List<Map<String, Object>> latestSstData;

    /** 最新CHL观测点数据 */
    private List<Map<String, Object>> latestChlData;
}
