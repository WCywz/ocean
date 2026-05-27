package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ocean.entity.UserSetting;
import com.ocean.mapper.UserSettingMapper;
import com.ocean.service.UserSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserSettingServiceImpl implements UserSettingService {

    @Autowired
    private UserSettingMapper userSettingMapper;

    private static final Map<String, String> DEFAULTS = Map.of(
            "sms_enabled", "true",
            "push_enabled", "true"
    );

    @Override
    public Map<String, String> getUserSettings(Long userId) {
        List<UserSetting> records = listByUserId(userId);
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        for (UserSetting r : records) {
            result.put(r.getSettingKey(), r.getSettingValue());
        }
        return result;
    }

    @Override
    public void updateSettings(Long userId, Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            LambdaUpdateWrapper<UserSetting> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(UserSetting::getUserId, userId)
                   .eq(UserSetting::getSettingKey, entry.getKey())
                   .set(UserSetting::getSettingValue, entry.getValue());
            int rows = userSettingMapper.update(null, wrapper);
            if (rows == 0) {
                UserSetting us = new UserSetting();
                us.setUserId(userId);
                us.setSettingKey(entry.getKey());
                us.setSettingValue(entry.getValue());
                try {
                    userSettingMapper.insert(us);
                } catch (DuplicateKeyException e) {
                    userSettingMapper.update(null, wrapper);
                }
            }
        }
    }

    @Override
    public List<UserSetting> listByUserId(Long userId) {
        return userSettingMapper.selectList(
                new LambdaQueryWrapper<UserSetting>()
                        .eq(UserSetting::getUserId, userId));
    }
}
