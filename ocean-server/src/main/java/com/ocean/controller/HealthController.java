package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.entity.HealthZone;
import com.ocean.service.HealthService;
import com.ocean.service.SystemConfigService;
import com.ocean.vo.ZoneHealthVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthService healthService;

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/zones")
    public Result<List<HealthZone>> getZones() {
        return Result.success(healthService.getZones());
    }

    @GetMapping("/assessment")
    public Result<ZoneHealthVO> getAssessment(@RequestParam(required = false) String forecastDate) {
        LocalDate date = forecastDate != null ? LocalDate.parse(forecastDate) : systemConfigService.getSystemDate();
        return Result.success(healthService.getAssessment(date));
    }

    @GetMapping("/assessment/{zoneId}/trend")
    public Result<List<Map<String, Object>>> getZoneTrend(
            @PathVariable Long zoneId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.success(healthService.getZoneTrend(zoneId, LocalDate.parse(startDate), LocalDate.parse(endDate)));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        return Result.success(healthService.getDashboard());
    }
}
