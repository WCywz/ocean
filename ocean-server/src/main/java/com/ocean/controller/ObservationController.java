package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.OceanDataQueryDTO;
import com.ocean.service.ObservationGridService;
import com.ocean.service.ObservationService;
import com.ocean.vo.OceanDataVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observation")
public class ObservationController {

    @Autowired
    private ObservationService observationService;

    @Autowired
    private ObservationGridService observationGridService;

    // ---- observation_data (原始观测数据) ----

    @GetMapping("/page")
    public Result<IPage<OceanDataVO>> getDataPage(OceanDataQueryDTO dto) {
        return Result.success(observationService.getDataPage(dto));
    }

    @GetMapping("/locations")
    public Result<List<Map<String, Object>>> getLocations() {
        return Result.success(observationService.getDistinctLocations());
    }

    @GetMapping("/sst-timeseries")
    public Result<List<Map<String, Object>>> getSstTimeSeries(
            @RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        return Result.success(observationService.getSstTimeSeries(startDate, endDate, lat, lon));
    }

    @GetMapping("/chl-timeseries")
    public Result<List<Map<String, Object>>> getChlTimeSeries(
            @RequestParam String startDate, @RequestParam String endDate,
            @RequestParam(required = false) Double lat, @RequestParam(required = false) Double lon) {
        return Result.success(observationService.getChlTimeSeries(startDate, endDate, lat, lon));
    }

    @GetMapping("/chl-by-depth")
    public Result<List<Map<String, Object>>> getChlByDepth() {
        return Result.success(observationService.getChlByDepth());
    }

    // ---- observation_grid (网格观测数据) ----

    @GetMapping("/map/grid")
    public Result<List<Map<String, Object>>> getMapGrid(
            @RequestParam String dataType,
            @RequestParam String obsDate,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat) {
        return Result.success(observationGridService.getMapGrid(
                dataType.toLowerCase(), obsDate, minLon, maxLon, minLat, maxLat));
    }

    @GetMapping("/trend/point")
    public Result<List<Map<String, Object>>> getPointTrend(
            @RequestParam String dataType,
            @RequestParam Double lon,
            @RequestParam Double lat,
            @RequestParam(required = false) String dateStart,
            @RequestParam(required = false) String dateEnd) {
        return Result.success(observationGridService.getPointTrend(
                dataType.toLowerCase(), lon, lat, dateStart, dateEnd));
    }

    @GetMapping("/grid/locations")
    public Result<List<Map<String, Object>>> getGridLocations() {
        return Result.success(observationGridService.getDistinctLocations());
    }
}
