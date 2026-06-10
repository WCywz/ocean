package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.service.ForecastService;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forecast")
public class ForecastController {

    @Autowired
    private ForecastService forecastService;

    @Autowired
    private Scheduler scheduler;

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard() {
        return Result.success(forecastService.getDashboard());
    }

    @GetMapping("/page")
    public Result<IPage<ForecastVO>> getRecordPage(ForecastQueryDTO dto) {
        return Result.success(forecastService.getRecordPage(dto));
    }

    @GetMapping("/sst/trend")
    public Result<List<Map<String, Object>>> getSstTrend(
            @RequestParam(required = false) Double lon, @RequestParam(required = false) Double lat) {
        return Result.success(forecastService.getSstTrend(lon, lat));
    }

    @GetMapping("/chl/trend")
    public Result<List<Map<String, Object>>> getChlTrend(
            @RequestParam(required = false) Double lon, @RequestParam(required = false) Double lat) {
        return Result.success(forecastService.getChlTrend(lon, lat));
    }

    @GetMapping("/locations")
    public Result<List<Map<String, Object>>> getLocations() {
        return Result.success(forecastService.getLocations());
    }

    @GetMapping("/map/grid")
    public Result<List<Map<String, Object>>> getMapGrid(MapGridQueryDTO dto) {
        return Result.success(forecastService.getMapGrid(dto));
    }

    @GetMapping("/trend/point")
    public Result<List<Map<String, Object>>> getPointTrend(
            @RequestParam String dataType, @RequestParam Double lon, @RequestParam Double lat,
            @RequestParam(required = false) String dateStart, @RequestParam(required = false) String dateEnd) {
        return Result.success(forecastService.getPointTrend(dataType, lon, lat, dateStart, dateEnd));
    }

    @GetMapping("/trend/dashboard")
    public Result<List<Map<String, Object>>> getDashboardTrend(
            @RequestParam(defaultValue = "SST") String dataType, @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(forecastService.getDashboardTrend(dataType, days));
    }

    @GetMapping("/chl/probability")
    public Result<List<Map<String, Object>>> getChlProbability(
            @RequestParam String dateStart, @RequestParam(required = false) String dateEnd,
            @RequestParam(required = false) Double threshold) {
        return Result.success(forecastService.getChlProbability(dateStart, dateEnd, threshold));
    }

    @GetMapping("/sea-areas")
    public Result<List<Map<String, Object>>> getSeaAreas() {
        return Result.success(forecastService.getSeaAreas());
    }

    @PostMapping("/run")
    public Result<Map<String, Object>> runForecast() {
        return Result.success(forecastService.runForecast());
    }

    /** 测试：按模型版本触发预报 */
    @PostMapping("/run/{versionId}")
    public Result<Map<String, Object>> runForecastForVersion(
            @PathVariable Long versionId,
            @RequestParam(defaultValue = "1") Long modelId,
            @RequestParam(required = false) String modelType) {
        return Result.success(forecastService.runForecast(versionId, modelId, modelType));
    }

    /** 测试：通过 Quartz 触发 ModelForecastJob */
    @PostMapping("/trigger/{versionId}")
    public Result<Map<String, Object>> triggerQuartzJob(@PathVariable Long versionId) {
        try {
            JobKey jobKey = new JobKey("forecast-v" + versionId, "model-forecast");
            if (!scheduler.checkExists(jobKey)) {
                return Result.error("Job 不存在，请先注册调度: forecast-v" + versionId);
            }
            scheduler.triggerJob(jobKey);
            return Result.success(Map.of("message", "Job 已触发: forecast-v" + versionId));
        } catch (SchedulerException e) {
            return Result.error("触发失败: " + e.getMessage());
        }
    }
}
