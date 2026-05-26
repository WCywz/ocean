package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.HealthRecord;
import com.ocean.entity.HealthZone;
import com.ocean.entity.MonitoringStation;
import com.ocean.mapper.ForecastGridMapper;
import com.ocean.mapper.HealthRecordMapper;
import com.ocean.mapper.HealthZoneMapper;
import com.ocean.mapper.MonitoringStationMapper;
import com.ocean.mapper.ObservationGridMapper;
import com.ocean.service.HealthService;
import com.ocean.service.SystemConfigService;
import com.ocean.vo.ZoneHealthVO;
import com.ocean.vo.ZoneHealthV2VO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDate;

@Service
public class HealthServiceImpl implements HealthService {

    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private HealthRecordMapper healthRecordMapper;
    @Autowired private ForecastGridMapper forecastGridMapper;
    @Autowired private ObservationGridMapper observationGridMapper;
    @Autowired private MonitoringStationMapper stationMapper;
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

            int month = LocalDate.parse(dateStr).getMonthValue();
            Double baseline = forecastGridMapper.selectZoneSstBaseline(
                    zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat(), month);
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

    private static final String TEMP_VAR = "thetao";
    private static final String CHL_VAR = "chl";
    private static final String FC_SST = "sst";
    private static final String FC_CHL = "chl";

    @Override
    public ZoneHealthV2VO getAssessmentV2(LocalDate date, int lookback, int lookahead) {
        if (date == null) date = systemConfigService.getSystemDate();
        List<HealthZone> zones = getZones();
        List<Map<String, Object>> zoneData = new ArrayList<>();

        for (HealthZone zone : zones) {
            Map<String, Object> z = new HashMap<>();
            z.put("id", zone.getId());
            z.put("label", zone.getZoneName());

            // recent: past N days from health_record (fallback: observation_grid)
            List<Map<String, Object>> recent = new ArrayList<>();
            for (int i = lookback; i >= 1; i--) {
                LocalDate d = date.minusDays(i);
                recent.add(buildDayFromRecordOrObs(zone, d));
            }
            z.put("recent", recent);

            // current: focus date from health_record (fallback: observation_grid)
            z.put("current", buildDayFromRecordOrObs(zone, date));

            // forecast: next N days from forecast_grid
            List<Map<String, Object>> forecast = new ArrayList<>();
            for (int i = 1; i <= lookahead; i++) {
                LocalDate d = date.plusDays(i);
                forecast.add(buildDayFromForecast(zone, d));
            }
            z.put("forecast", forecast);

            zoneData.add(z);
        }

        ZoneHealthV2VO vo = new ZoneHealthV2VO();
        vo.setZones(zoneData);
        return vo;
    }

    private Map<String, Object> buildDayFromRecordOrObs(HealthZone zone, LocalDate date) {
        String dateStr = date.toString();
        HealthRecord record = healthRecordMapper.selectOne(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getZoneId, zone.getId())
                        .eq(HealthRecord::getAssessDate, date));

        if (record != null) {
            return buildDayFromRecord(record);
        }
        return buildDayFromObsGrid(zone, dateStr);
    }

    private Map<String, Object> buildDayFromRecord(HealthRecord record) {
        Map<String, Object> day = new HashMap<>();
        day.put("date", record.getAssessDate().toString());
        day.put("overallGrade", record.getOverallGrade());

        Map<String, Object> sst = new HashMap<>();
        sst.put("avg", record.getSstAvg());
        sst.put("max", record.getSstMax());
        sst.put("anomaly", record.getSstAnomaly());
        sst.put("trend", record.getSstTrend());
        day.put("sst", sst);

        Map<String, Object> chl = new HashMap<>();
        chl.put("avg", record.getChlAvg());
        chl.put("max", record.getChlMax());
        chl.put("trend", record.getChlTrend());
        day.put("chl", chl);

        Map<String, Object> hw = new HashMap<>();
        hw.put("active", record.getHeatwaveActive() != null && record.getHeatwaveActive() == 1);
        hw.put("days", record.getHeatwaveDays() != null ? record.getHeatwaveDays() : 0);
        day.put("heatwave", hw);

        return day;
    }

    private Map<String, Object> buildDayFromObsGrid(HealthZone zone, String dateStr) {
        Map<String, Object> day = new HashMap<>();
        day.put("date", dateStr);

        Map<String, Object> tempStats = observationGridMapper.selectZoneStats(
                TEMP_VAR, dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
        Map<String, Object> chlStats = observationGridMapper.selectZoneStats(
                CHL_VAR, dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

        if (tempStats == null || tempStats.get("avg_val") == null) {
            day.put("overallGrade", "good");
            day.put("sst", emptyMetric());
            day.put("chl", emptyMetric());
            day.put("heatwave", Map.of("active", false, "days", 0));
            return day;
        }

        double tempAvg = ((Number) tempStats.get("avg_val")).doubleValue();
        double tempMax = ((Number) tempStats.get("max_val")).doubleValue();
        double chlAvg = chlStats != null && chlStats.get("avg_val") != null
                ? ((Number) chlStats.get("avg_val")).doubleValue() : 0;
        double chlMax = chlStats != null && chlStats.get("max_val") != null
                ? ((Number) chlStats.get("max_val")).doubleValue() : 0;

        int month = LocalDate.parse(dateStr).getMonthValue();
        Double baseline = observationGridMapper.selectZoneBaseline(
                TEMP_VAR, month,
                zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
        double anomaly = baseline != null ? tempAvg - baseline : 0;

        String sstGrade = gradeSstValue(Math.abs(anomaly));
        String chlGrade = gradeChlValue(chlAvg);
        String overallGrade = worstOf(sstGrade, chlGrade);

        day.put("overallGrade", overallGrade);

        Map<String, Object> sst = new HashMap<>();
        sst.put("avg", tempAvg);
        sst.put("max", tempMax);
        sst.put("anomaly", anomaly);
        sst.put("trend", "stable");
        day.put("sst", sst);

        Map<String, Object> chl = new HashMap<>();
        chl.put("avg", chlAvg);
        chl.put("max", chlMax);
        chl.put("trend", "stable");
        day.put("chl", chl);

        day.put("heatwave", Map.of("active", false, "days", 0));

        return day;
    }

    private Map<String, Object> buildDayFromForecast(HealthZone zone, LocalDate date) {
        String dateStr = date.toString();
        Map<String, Object> day = new HashMap<>();
        day.put("date", dateStr);

        Map<String, Object> sstStats = forecastGridMapper.selectZoneStats(
                FC_SST, dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
        Map<String, Object> chlStats = forecastGridMapper.selectZoneStats(
                FC_CHL, dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

        if (sstStats == null || sstStats.get("avg_val") == null) {
            day.put("overallGrade", "good");
            day.put("sstAvg", 0d);
            day.put("chlAvg", 0d);
            return day;
        }

        double sstAvg = ((Number) sstStats.get("avg_val")).doubleValue();
        double chlAvg = chlStats != null && chlStats.get("avg_val") != null
                ? ((Number) chlStats.get("avg_val")).doubleValue() : 0;

        Double baseline = forecastGridMapper.selectZoneSstBaseline(
                zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat(), date.getMonthValue());
        double anomaly = baseline != null ? sstAvg - baseline : 0;

        String sstGrade = gradeSstValue(Math.abs(anomaly));
        String chlGrade = gradeChlValue(chlAvg);
        String overallGrade = worstOf(sstGrade, chlGrade);

        day.put("overallGrade", overallGrade);

        Map<String, Object> sst = new HashMap<>();
        sst.put("avg", sstAvg);
        sst.put("anomaly", anomaly);
        day.put("sst", sst);

        Map<String, Object> chl = new HashMap<>();
        chl.put("avg", chlAvg);
        day.put("chl", chl);

        return day;
    }

    @Override
    public Map<String, Object> getAlertMap(LocalDate date) {
        if (date == null) date = systemConfigService.getSystemDate();
        String dateStr = date.toString();
        int month = date.getMonthValue();

        Map<String, Object> result = new HashMap<>();

        // ---- stations ----
        List<MonitoringStation> stations = stationMapper.selectList(
                new LambdaQueryWrapper<MonitoringStation>().eq(MonitoringStation::getIsActive, 1));
        List<Map<String, Object>> stationList = new ArrayList<>();

        for (MonitoringStation s : stations) {
            Map<String, Object> sm = new HashMap<>();
            sm.put("stationName", s.getStationName());
            sm.put("lat", s.getLat());
            sm.put("lon", s.getLon());

            double tol = 0.13;
            Map<String, Object> sstGrid = observationGridMapper.selectZoneStats(
                    TEMP_VAR, dateStr, s.getLon() - tol, s.getLon() + tol, s.getLat() - tol, s.getLat() + tol);
            Map<String, Object> chlGrid = observationGridMapper.selectZoneStats(
                    CHL_VAR, dateStr, s.getLon() - tol, s.getLon() + tol, s.getLat() - tol, s.getLat() + tol);

            if (sstGrid == null || sstGrid.get("avg_val") == null) continue;

            double sstAvg = ((Number) sstGrid.get("avg_val")).doubleValue();
            double chlAvg = chlGrid != null && chlGrid.get("avg_val") != null
                    ? ((Number) chlGrid.get("avg_val")).doubleValue() : 0;

            Double sstBaseline = observationGridMapper.selectZoneBaseline(
                    TEMP_VAR, month, s.getLon() - tol, s.getLon() + tol, s.getLat() - tol, s.getLat() + tol);
            double anomaly = sstBaseline != null ? sstAvg - sstBaseline : 0;

            String sstGrade = gradeSstValue(Math.abs(anomaly));
            String chlGrade = gradeChlValue(chlAvg);
            String overallGrade = worstOf(sstGrade, chlGrade);

            sm.put("sstValue", Math.round(sstAvg * 10.0) / 10.0);
            sm.put("sstAnomaly", Math.round(anomaly * 10.0) / 10.0);
            sm.put("sstGrade", sstGrade);
            sm.put("chlValue", Math.round(chlAvg * 10.0) / 10.0);
            sm.put("chlGrade", chlGrade);
            sm.put("overallGrade", overallGrade);

            stationList.add(sm);
        }
        result.put("stations", stationList);

        // ---- hotspots ----
        List<Map<String, Object>> rawSpots = observationGridMapper.selectHotspots(dateStr, month, 1.5);
        List<Map<String, Object>> hotspots = new ArrayList<>();
        java.util.Set<String> seenCells = new java.util.HashSet<>();
        for (Map<String, Object> spot : rawSpots) {
            double lat = ((Number) spot.get("lat")).doubleValue();
            double lon = ((Number) spot.get("lon")).doubleValue();
            double anomaly = ((Number) spot.get("anomaly")).doubleValue();
            String cellKey = String.format("%.2f,%.2f",
                    Math.round(lat * 4.0) / 4.0, Math.round(lon * 4.0) / 4.0);
            if (seenCells.contains(cellKey)) continue;
            seenCells.add(cellKey);

            Map<String, Object> h = new HashMap<>();
            h.put("lat", lat);
            h.put("lon", lon);
            h.put("variable", "thetao");
            h.put("value", Math.round(((Number) spot.get("value")).doubleValue() * 10.0) / 10.0);
            h.put("anomaly", Math.round(anomaly * 10.0) / 10.0);
            h.put("grade", gradeSstValue(Math.abs(anomaly)));
            hotspots.add(h);
            if (hotspots.size() >= 5) break;
        }
        result.put("hotspots", hotspots);

        // ---- summary ----
        long badCount = stationList.stream().filter(s -> "bad".equals(s.get("overallGrade"))).count();
        long warnCount = stationList.stream().filter(s -> "warn".equals(s.get("overallGrade"))).count();
        long fineCount = stationList.stream().filter(s -> "fine".equals(s.get("overallGrade"))).count();
        long goodCount = stationList.stream().filter(s -> "good".equals(s.get("overallGrade"))).count();

        StringBuilder sb = new StringBuilder();
        sb.append(stationList.size()).append(" 个站点中 ");
        boolean hasPrev = false;
        if (badCount > 0) { sb.append(badCount).append(" 差"); hasPrev = true; }
        if (warnCount > 0) { if (hasPrev) sb.append(" "); sb.append(warnCount).append(" 中"); hasPrev = true; }
        if (fineCount > 0) { if (hasPrev) sb.append(" "); sb.append(fineCount).append(" 良"); hasPrev = true; }
        if (goodCount > 0) { if (hasPrev) sb.append(" "); sb.append(goodCount).append(" 优"); }
        if (hotspots.size() > 0) {
            sb.append("，").append(hotspots.size()).append(" 个热点网格 SST 异常超标");
        }
        result.put("summary", sb.toString());

        return result;
    }
}
