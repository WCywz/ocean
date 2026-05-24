package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.HealthRecord;
import com.ocean.entity.HealthZone;
import com.ocean.mapper.ForecastGridMapper;
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
    @Autowired private ForecastGridMapper forecastGridMapper;
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

    private static final double SST_ANOMALY_BAD = 2.5;
    private static final double SST_ANOMALY_WARN = 1.5;
    private static final double SST_ANOMALY_FINE = 0.5;
    private static final double CHL_BAD = 5.0;
    private static final double CHL_WARN = 3.0;
    private static final double CHL_FINE = 2.0;

    @Override
    public String buildDailySummary() {
        List<HealthZone> zones = getZones();
        if (zones.isEmpty()) return null;

        LocalDate today = systemConfigService.getSystemDate();
        LocalDate tomorrow = today.plusDays(1);
        String tomorrowStr = tomorrow.toString();

        List<String> problems = new ArrayList<>();
        int totalZones = zones.size();
        int goodCount = 0;

        for (HealthZone zone : zones) {
            // Today: read from health_record
            HealthRecord todayRecord = healthRecordMapper.selectOne(
                    new LambdaQueryWrapper<HealthRecord>()
                            .eq(HealthRecord::getZoneId, zone.getId())
                            .eq(HealthRecord::getAssessDate, today));
            String todayGrade = todayRecord != null ? todayRecord.getOverallGrade() : null;

            // Tomorrow: estimate from forecast_grid
            String tomorrowGrade = estimateTomorrowGrade(zone, tomorrowStr);

            // Use the worse of today and tomorrow for alert decision
            String effectiveGrade = worstOf(todayGrade, tomorrowGrade);

            if (effectiveGrade == null) {
                continue;
            }

            if ("good".equals(effectiveGrade) || "fine".equals(effectiveGrade)) {
                goodCount++;
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append(zone.getZoneName()).append("：");
                sb.append(gradeLabel(effectiveGrade));

                if (todayRecord != null) {
                    List<String> reasons = new ArrayList<>();
                    if ("bad".equals(todayRecord.getSstGrade()) || "warn".equals(todayRecord.getSstGrade())) {
                        reasons.add("SST异常偏高" + String.format("%.1f", todayRecord.getSstAnomaly()) + "℃");
                    }
                    if ("bad".equals(todayRecord.getChlGrade()) || "warn".equals(todayRecord.getChlGrade())) {
                        reasons.add("chl偏高" + String.format("%.1f", todayRecord.getChlAvg()));
                    }
                    if (todayRecord.getHeatwaveActive() != null && todayRecord.getHeatwaveActive() == 1) {
                        reasons.add("热浪持续" + todayRecord.getHeatwaveDays() + "天");
                    }
                    if (!reasons.isEmpty()) {
                        sb.append("（").append(String.join("，", reasons)).append("）");
                    }
                }

                if (tomorrowGrade != null && !tomorrowGrade.equals(todayGrade)) {
                    sb.append(" 明日预计").append(gradeLabel(tomorrowGrade));
                }

                problems.add(sb.toString());
            }
        }

        if (problems.isEmpty() || goodCount == totalZones) {
            return "今日各海域健康状态良好，无需关注。";
        }

        return String.join(" ", problems);
    }

    private String estimateTomorrowGrade(HealthZone zone, String dateStr) {
        try {
            Map<String, Object> sstStats = forecastGridMapper.selectZoneStats(
                    "sst", dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
            Map<String, Object> chlStats = forecastGridMapper.selectZoneStats(
                    "chl", dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

            if (sstStats == null || sstStats.get("avg_val") == null) return null;

            double sstAvg = ((Number) sstStats.get("avg_val")).doubleValue();
            double chlAvg = chlStats != null && chlStats.get("avg_val") != null
                    ? ((Number) chlStats.get("avg_val")).doubleValue() : 0;

            Double baseline = forecastGridMapper.selectZoneSstBaseline(
                    zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
            double anomaly = baseline != null ? Math.abs(sstAvg - baseline) : 0;

            String sstGrade = gradeSstValue(anomaly);
            String chlGrade = gradeChlValue(chlAvg);
            return worstOf(sstGrade, chlGrade);
        } catch (Exception e) {
            return null;
        }
    }

    private String gradeSstValue(double absAnomaly) {
        if (absAnomaly > SST_ANOMALY_BAD) return "bad";
        if (absAnomaly > SST_ANOMALY_WARN) return "warn";
        if (absAnomaly > SST_ANOMALY_FINE) return "fine";
        return "good";
    }

    private String gradeChlValue(double avg) {
        if (avg >= CHL_BAD) return "bad";
        if (avg >= CHL_WARN) return "warn";
        if (avg >= CHL_FINE) return "fine";
        return "good";
    }

    private String worstOf(String a, String b) {
        if (a == null) return b;
        if (b == null) return a;
        List<String> order = List.of("good", "fine", "warn", "bad");
        return order.indexOf(a) > order.indexOf(b) ? a : b;
    }

    private String gradeLabel(String grade) {
        return switch (grade) {
            case "good" -> "优";
            case "fine" -> "良";
            case "warn" -> "中";
            case "bad" -> "差";
            default -> grade;
        };
    }
}
