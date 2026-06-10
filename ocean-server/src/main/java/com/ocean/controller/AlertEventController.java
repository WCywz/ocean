package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.service.AlertEventService;
import com.ocean.vo.AlertEventVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alert")
public class AlertEventController {

    @Autowired
    private AlertEventService alertEventService;

    @GetMapping("/unread")
    public Result<List<AlertEventVO>> getUnreadAlerts() {
        return Result.success(alertEventService.getUnreadAlerts());
    }

    @GetMapping("/recent")
    public Result<List<AlertEventVO>> getRecentAlerts(@RequestParam(defaultValue = "20") Integer limit) {
        return Result.success(alertEventService.getRecentAlerts(limit));
    }

    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount() {
        return Result.success(alertEventService.getUnreadCount());
    }

    @PutMapping("/{id}/read")
    public Result<?> markAsRead(@PathVariable Long id) {
        alertEventService.markAsRead(id);
        return Result.success("已标记为已读");
    }

    @PutMapping("/read-all")
    public Result<?> markAllAsRead() {
        alertEventService.markAllAsRead();
        return Result.success("全部标记为已读");
    }

    @GetMapping("/page")
    public Result<IPage<AlertEventVO>> getAlertPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Long modelId,
            @RequestParam(required = false) String alertType,
            @RequestParam(required = false) Integer isRead) {
        return Result.success(alertEventService.getAlertPage(pageNum, pageSize,
                startTime, endTime, modelId, alertType, isRead));
    }
}
