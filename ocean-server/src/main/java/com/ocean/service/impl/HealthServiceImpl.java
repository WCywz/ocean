package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.HealthRecord;
import com.ocean.entity.HealthZone;
import com.ocean.mapper.HealthRecordMapper;
import com.ocean.mapper.HealthZoneMapper;
import com.ocean.service.HealthService;
import com.ocean.service.SystemConfigService;
import com.ocean.vo.ZoneHealthVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDate;

@Service
public class HealthServiceImpl implements HealthService {

    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private HealthRecordMapper healthRecordMapper;
    @Autowired private SystemConfigService systemConfigService;

    @Override
    public List<HealthZone> getZones() {
        return healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1).orderByAsc(HealthZone::getSortOrder));
    }

    @Override
    public ZoneHealthVO getAssessment(LocalDate date) {
        if (date == null) date = systemConfigService.getSystemDate();
        LocalDate assessDate = date;
        List<HealthZone> zones = getZones();
        List<Map<String, Object>> zoneData = new ArrayList<>();
        for (HealthZone zone : zones) {
            HealthRecord record = healthRecordMapper.selectOne(
                    new LambdaQueryWrapper<HealthRecord>()
                            .eq(HealthRecord::getZoneId, zone.getId())
                            .eq(HealthRecord::getAssessDate, assessDate));
            Map<String, Object> z = new HashMap<>();
            z.put("id", zone.getId());
            z.put("label", zone.getZoneName());
            if (record != null) {
                Map<String, Object> sst = new HashMap<>();
                sst.put("avg", record.getSstAvg());
                sst.put("max", record.getSstMax());
                sst.put("anomaly", record.getSstAnomaly());
                sst.put("trend", record.getSstTrend());
                z.put("sst", sst);
                Map<String, Object> chl = new HashMap<>();
                chl.put("avg", record.getChlAvg());
                chl.put("max", record.getChlMax());
                chl.put("trend", record.getChlTrend());
                z.put("chl", chl);
                Map<String, Object> hw = new HashMap<>();
                hw.put("active", record.getHeatwaveActive() != null && record.getHeatwaveActive() == 1);
                hw.put("days", record.getHeatwaveDays() != null ? record.getHeatwaveDays() : 0);
                z.put("heatwave", hw);
                z.put("overallGrade", record.getOverallGrade());
            } else {
                z.put("sst", emptyMetric());
                z.put("chl", emptyMetric());
                z.put("heatwave", Map.of("active", false, "days", 0));
                z.put("overallGrade", "good");
            }
            zoneData.add(z);
        }
        ZoneHealthVO vo = new ZoneHealthVO();
        vo.setZones(zoneData);
        return vo;
    }

    private Map<String, Object> emptyMetric() {
        Map<String, Object> m = new HashMap<>();
        m.put("avg", 0d); m.put("max", 0d); m.put("anomaly", 0d); m.put("trend", "stable");
        return m;
    }

    @Override
    public List<Map<String, Object>> getZoneTrend(Long zoneId, LocalDate startDate, LocalDate endDate) {
        List<HealthRecord> records = healthRecordMapper.selectList(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getZoneId, zoneId)
                        .between(HealthRecord::getAssessDate, startDate, endDate)
                        .orderByAsc(HealthRecord::getAssessDate));
        List<Map<String, Object>> result = new ArrayList<>();
        for (HealthRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("assessDate", r.getAssessDate().toString());
            m.put("sstAvg", r.getSstAvg());
            m.put("sstAnomaly", r.getSstAnomaly());
            m.put("chlAvg", r.getChlAvg());
            m.put("heatwaveActive", r.getHeatwaveActive());
            m.put("heatwaveDays", r.getHeatwaveDays());
            m.put("overallGrade", r.getOverallGrade());
            result.add(m);
        }
        return result;
    }

    @Override
    public Map<String, Object> getDashboard() {
        List<HealthZone> zones = getZones();
        LocalDate today = systemConfigService.getSystemDate();
        List<Map<String, Object>> zoneHealth = new ArrayList<>();
        for (HealthZone zone : zones) {
            HealthRecord record = healthRecordMapper.selectOne(
                    new LambdaQueryWrapper<HealthRecord>()
                            .eq(HealthRecord::getZoneId, zone.getId())
                            .eq(HealthRecord::getAssessDate, today));
            Map<String, Object> z = new HashMap<>();
            z.put("id", zone.getId());
            z.put("name", zone.getZoneName());
            z.put("grade", record != null ? record.getOverallGrade() : "good");
            zoneHealth.add(z);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("zones", zoneHealth);
        return result;
    }
}
