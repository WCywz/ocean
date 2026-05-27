package com.ocean.service;

import com.ocean.entity.UserSetting;

import java.util.List;
import java.util.Map;

public interface UserSettingService {

    Map<String, String> getUserSettings(Long userId);

    void updateSettings(Long userId, Map<String, String> settings);

    List<UserSetting> listByUserId(Long userId);
}
