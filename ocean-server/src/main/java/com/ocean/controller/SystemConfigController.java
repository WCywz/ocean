package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemConfigController {

    @Autowired
    private SystemConfigService systemConfigService;

    @GetMapping("/date")
    public Result<String> getSystemDate() {
        return Result.success(systemConfigService.getSystemDate().toString());
    }
}
