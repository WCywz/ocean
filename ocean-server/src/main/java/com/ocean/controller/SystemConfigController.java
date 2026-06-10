package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/date")
    public Result<String> getSystemDate() {
        return Result.success(systemConfigService.getSystemDate().toString());
    }

    @PostMapping("/date/advance")
    public Result<Map<String, Object>> advanceDate() {
        systemConfigService.advanceDay();
        String newDate = systemConfigService.getSystemDate().toString();
        return Result.success(Map.of("systemDate", newDate));
    }

    @PutMapping("/date")
    public Result<Map<String, Object>> setDate(@RequestParam String date) {
        systemConfigService.setDate(java.time.LocalDate.parse(date));
        return Result.success(Map.of("systemDate", date));
    }
}
