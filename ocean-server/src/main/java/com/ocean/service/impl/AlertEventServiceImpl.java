package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.entity.AlertEvent;
import com.ocean.mapper.AlertEventMapper;
import com.ocean.service.AlertEventService;
import com.ocean.vo.AlertEventVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AlertEventServiceImpl implements AlertEventService {

    @Autowired
    private AlertEventMapper alertEventMapper;

    @Override
    public List<AlertEventVO> getUnreadAlerts() {
        return alertEventMapper.selectList(
                new LambdaQueryWrapper<AlertEvent>()
                        .eq(AlertEvent::getIsRead, 0)
                        .orderByDesc(AlertEvent::getCreateTime))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<AlertEventVO> getRecentAlerts(Integer limit) {
        int n = (limit != null && limit > 0) ? Math.min(limit, 100) : 20;
        return alertEventMapper.selectList(
                new LambdaQueryWrapper<AlertEvent>()
                        .orderByDesc(AlertEvent::getCreateTime)
                        .last("LIMIT " + n))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getUnreadCount() {
        Long count = alertEventMapper.selectCount(
                new LambdaQueryWrapper<AlertEvent>().eq(AlertEvent::getIsRead, 0));
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        return result;
    }

    @Override
    public void markAsRead(Long id) {
        AlertEvent event = alertEventMapper.selectById(id);
        if (event != null && event.getIsRead() == 0) {
            event.setIsRead(1);
            alertEventMapper.updateById(event);
        }
    }

    @Override
    public void markAllAsRead() {
        List<AlertEvent> unread = alertEventMapper.selectList(
                new LambdaQueryWrapper<AlertEvent>().eq(AlertEvent::getIsRead, 0));
        for (AlertEvent e : unread) {
            e.setIsRead(1);
            alertEventMapper.updateById(e);
        }
    }

    @Override
    public IPage<AlertEventVO> getAlertPage(Integer pageNum, Integer pageSize,
                                             LocalDateTime startTime, LocalDateTime endTime,
                                             Long modelId, String alertType, Integer isRead) {
        LambdaQueryWrapper<AlertEvent> wrapper = new LambdaQueryWrapper<>();
        if (startTime != null) {
            wrapper.ge(AlertEvent::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(AlertEvent::getCreateTime, endTime);
        }
        if (modelId != null) {
            wrapper.eq(AlertEvent::getModelId, modelId);
        }
        if (alertType != null && !alertType.isEmpty()) {
            wrapper.eq(AlertEvent::getAlertType, alertType);
        }
        if (isRead != null) {
            wrapper.eq(AlertEvent::getIsRead, isRead);
        }
        wrapper.orderByDesc(AlertEvent::getCreateTime);

        Page<AlertEvent> page = new Page<>(pageNum, pageSize);
        Page<AlertEvent> result = alertEventMapper.selectPage(page, wrapper);
        return result.convert(this::toVO);
    }

    private AlertEventVO toVO(AlertEvent e) {
        AlertEventVO vo = new AlertEventVO();
        BeanUtils.copyProperties(e, vo);
        return vo;
    }
}
