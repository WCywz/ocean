package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.service.OceanDataService;
import com.ocean.vo.OceanDataVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 海洋观测数据控制器
 */
// @RestController -- disabled, replaced by ObservationController
// @RequestMapping("/api/ocean-data")
public class OceanDataController {

    //@Autowired
    private OceanDataService oceanDataService;

    /**
     * 分页查询观测数据
     */
    @GetMapping("/page")
    public Result<IPage<OceanDataVO>> getDataPage(OceanDataQueryDTO dto) {
        IPage<OceanDataVO> page = oceanDataService.getDataPage(dto);
        return Result.success(page);
    }

    /**
     * 获取所有去重的经纬度
     */
    @GetMapping("/locations")
    public Result<List<Map<String, Object>>> getDistinctLonLat() {
        List<Map<String, Object>> data = oceanDataService.getDistinctLonLat();
        return Result.success(data);
    }

    /**
     * 海表温度时间序列（支持经纬度过滤）
     */
    @GetMapping("/sst-timeseries")
    public Result<List<Map<String, Object>>> getSstTimeSeries(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lon) {
        List<Map<String, Object>> data = oceanDataService.getSstTimeSeries(startDate, endDate, lat, lon);
        return Result.success(data);
    }

    /**
     * 叶绿素浓度时间序列（支持经纬度过滤）
     */
    @GetMapping("/chl-timeseries")
    public Result<List<Map<String, Object>>> getChlTimeSeries(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) BigDecimal lat,
            @RequestParam(required = false) BigDecimal lon) {
        List<Map<String, Object>> data = oceanDataService.getChlTimeSeries(startDate, endDate, lat, lon);
        return Result.success(data);
    }

    /**
     * 按深度的叶绿素浓度分布
     */
    @GetMapping("/chl-by-depth")
    public Result<List<Map<String, Object>>> getChlByDepth() {
        List<Map<String, Object>> data = oceanDataService.getChlByDepth();
        return Result.success(data);
    }
}
