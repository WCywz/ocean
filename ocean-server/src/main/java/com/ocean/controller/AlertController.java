package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.AlertRuleSaveDTO;
import com.ocean.entity.AlertRule;
import com.ocean.service.AlertService;
import com.ocean.vo.AlertEventVO;
import com.ocean.vo.AlertStationDetailVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/alert")
public class AlertController {

    @Autowired
    private AlertService alertService;

    @GetMapping("/rules")
    public Result<List<AlertRule>> getRules() {
        return Result.success(alertService.getRules());
    }

    @PostMapping("/rules")
    public Result<?> addRule(@RequestBody AlertRuleSaveDTO dto) {
        alertService.addRule(dto);
        return Result.success("规则创建成功");
    }

    @PutMapping("/rules/{id}")
    public Result<?> updateRule(@PathVariable Long id, @RequestBody AlertRuleSaveDTO dto) {
        alertService.updateRule(id, dto);
        return Result.success("规则更新成功");
    }

    @GetMapping("/events")
    public Result<List<AlertEventVO>> getEvents(
            @RequestParam(required = false) String alertDate,
            @RequestParam(required = false) Long zoneId,
            @RequestParam(defaultValue = "active") String status) {
        LocalDate date = alertDate != null ? LocalDate.parse(alertDate) : null;
        return Result.success(alertService.getEvents(date, zoneId, status));
    }

    @GetMapping("/events/{id}")
    public Result<AlertEventVO> getEvent(@PathVariable Long id) {
        return Result.success(alertService.getEventById(id));
    }

    @GetMapping("/events/{id}/stations")
    public Result<List<AlertStationDetailVO>> getEventStations(@PathVariable Long id) {
        return Result.success(alertService.getEventStations(id));
    }

    @PutMapping("/events/{id}/acknowledge")
    public Result<?> acknowledge(@PathVariable Long id, @RequestParam(defaultValue = "1") Long userId) {
        alertService.acknowledgeEvent(id, userId);
        return Result.success("已确认");
    }

    @PostMapping("/events/generate")
    public Result<?> generateAlerts(@RequestParam String alertDate) {
        int count = alertService.generateAlerts(LocalDate.parse(alertDate));
        return Result.success("生成告警 " + count + " 条");
    }
}
