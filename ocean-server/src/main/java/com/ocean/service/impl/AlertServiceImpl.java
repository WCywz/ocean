package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.BusinessException;
import com.ocean.dto.AlertRuleSaveDTO;
import com.ocean.entity.*;
import com.ocean.mapper.*;
import com.ocean.service.AlertService;
import com.ocean.vo.AlertEventVO;
import com.ocean.vo.AlertStationDetailVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AlertServiceImpl implements AlertService {

    @Autowired private AlertRuleMapper alertRuleMapper;
    @Autowired private AlertEventMapper alertEventMapper;
    @Autowired private AlertStationDetailMapper alertStationDetailMapper;
    @Autowired private ForecastGridMapper forecastGridMapper;
    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private MonitoringStationMapper stationMapper;

    @Override
    public List<AlertRule> getRules() {
        return alertRuleMapper.selectList(new LambdaQueryWrapper<AlertRule>().eq(AlertRule::getIsActive, 1));
    }

    @Override
    public void addRule(AlertRuleSaveDTO dto) {
        AlertRule rule = new AlertRule();
        BeanUtils.copyProperties(dto, rule);
        rule.setIsActive(1);
        alertRuleMapper.insert(rule);
    }

    @Override
    public void updateRule(Long id, AlertRuleSaveDTO dto) {
        AlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null) throw new BusinessException("规则不存在");
        BeanUtils.copyProperties(dto, rule);
        alertRuleMapper.updateById(rule);
    }

    @Override
    public List<AlertEventVO> getEvents(LocalDate alertDate, Long zoneId, String status) {
        LambdaQueryWrapper<AlertEvent> wrapper = new LambdaQueryWrapper<>();
        if (alertDate != null) wrapper.eq(AlertEvent::getAlertDate, alertDate);
        if (zoneId != null) wrapper.eq(AlertEvent::getZoneId, zoneId);
        if (status != null) wrapper.eq(AlertEvent::getStatus, status);
        wrapper.orderByDesc(AlertEvent::getCreateTime);
        return alertEventMapper.selectList(wrapper).stream().map(this::toEventVO).collect(Collectors.toList());
    }

    @Override
    public AlertEventVO getEventById(Long id) {
        AlertEvent event = alertEventMapper.selectById(id);
        if (event == null) throw new BusinessException("告警事件不存在");
        return toEventVO(event);
    }

    @Override
    public List<AlertStationDetailVO> getEventStations(Long eventId) {
        List<AlertStationDetail> details = alertStationDetailMapper.selectList(
                new LambdaQueryWrapper<AlertStationDetail>().eq(AlertStationDetail::getAlertId, eventId));
        List<AlertStationDetailVO> result = new ArrayList<>();
        for (AlertStationDetail d : details) {
            AlertStationDetailVO vo = new AlertStationDetailVO();
            BeanUtils.copyProperties(d, vo);
            MonitoringStation station = stationMapper.selectById(d.getStationId());
            if (station != null) {
                vo.setStationName(station.getStationName());
                vo.setLat(station.getLat());
                vo.setLon(station.getLon());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public void acknowledgeEvent(Long id, Long userId) {
        AlertEvent event = alertEventMapper.selectById(id);
        if (event == null) throw new BusinessException("告警事件不存在");
        event.setStatus("acknowledged");
        event.setAckBy(userId);
        event.setAckAt(LocalDateTime.now());
        alertEventMapper.updateById(event);
    }

    @Override
    @Transactional
    public int generateAlerts(LocalDate alertDate) {
        int count = 0;
        List<AlertRule> rules = getRules();
        List<HealthZone> zones = healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1));
        for (AlertRule rule : rules) {
            for (HealthZone zone : zones) {
                List<Map<String, Object>> gridData = forecastGridMapper.selectByBbox(
                        rule.getVariable(), alertDate.toString(), 0.0,
                        zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
                if (gridData.isEmpty()) continue;
                double maxVal = 0, sumVal = 0;
                for (Map<String, Object> row : gridData) {
                    double val = ((Number) row.get("value")).doubleValue();
                    if (val > maxVal) maxVal = val;
                    sumVal += val;
                }
                AlertEvent event = new AlertEvent();
                event.setZoneId(zone.getId());
                event.setRuleId(rule.getId());
                event.setVariable(rule.getVariable());
                event.setSource(rule.getSource());
                event.setAlertDate(alertDate);
                event.setMaxValue(maxVal);
                event.setAvgValue(Math.round(sumVal / gridData.size() * 100.0) / 100.0);
                event.setThreshold(rule.getThreshold());
                event.setStationCount(0);
                event.setSeverity(rule.getSeverity());
                event.setStatus("active");
                event.setMessage(zone.getZoneName() + " " + rule.getVariable().toUpperCase() + " 数据，最高 " + String.format("%.2f", maxVal));
                alertEventMapper.insert(event);
                count++;
            }
        }
        return count;
    }

    private AlertEventVO toEventVO(AlertEvent event) {
        AlertEventVO vo = new AlertEventVO();
        BeanUtils.copyProperties(event, vo);
        HealthZone zone = healthZoneMapper.selectById(event.getZoneId());
        if (zone != null) vo.setZoneName(zone.getZoneName());
        if (event.getRuleId() != null) {
            AlertRule rule = alertRuleMapper.selectById(event.getRuleId());
            if (rule != null) vo.setRuleName(rule.getRuleName());
        }
        return vo;
    }
}
