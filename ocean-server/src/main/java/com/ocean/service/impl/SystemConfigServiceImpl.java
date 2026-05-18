package com.ocean.service.impl;

import com.ocean.entity.SystemConfig;
import com.ocean.mapper.SystemConfigMapper;
import com.ocean.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String KEY_SYSTEM_DATE = "system_date";

    @Autowired
    private SystemConfigMapper systemConfigMapper;

    @Override
    public LocalDate getSystemDate() {
        SystemConfig config = systemConfigMapper.selectById(KEY_SYSTEM_DATE);
        if (config == null || config.getConfigValue() == null) {
            return LocalDate.of(2026, 1, 1);
        }
        return LocalDate.parse(config.getConfigValue());
    }

    @Override
    public void advanceDay() {
        LocalDate current = getSystemDate();
        LocalDate next = current.plusDays(1);
        SystemConfig config = new SystemConfig();
        config.setConfigKey(KEY_SYSTEM_DATE);
        config.setConfigValue(next.toString());
        systemConfigMapper.updateById(config);
    }
}
