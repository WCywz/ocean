package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.vo.AlertEventVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AlertEventService {

    List<AlertEventVO> getUnreadAlerts();

    List<AlertEventVO> getRecentAlerts(Integer limit);

    Map<String, Object> getUnreadCount();

    void markAsRead(Long id);

    void markAllAsRead();

    IPage<AlertEventVO> getAlertPage(Integer pageNum, Integer pageSize,
                                      LocalDateTime startTime, LocalDateTime endTime,
                                      Long modelId, String alertType, Integer isRead);
}
