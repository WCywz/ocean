package com.ocean.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.HealthRecord;
import com.ocean.entity.HealthZone;
import com.ocean.mapper.HealthRecordMapper;
import com.ocean.mapper.HealthZoneMapper;
import com.ocean.mapper.ObservationGridMapper;
import com.ocean.service.PipelineLockService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Component
public class HealthAssessmentTask {

    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private HealthRecordMapper healthRecordMapper;
    @Autowired private ObservationGridMapper observationGridMapper;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private PipelineLockService pipelineLock;

    private static final double SST_TREND_THRESHOLD = 0.2;
    private static final double CHL_TREND_THRESHOLD = 0.1;
    private static final double HEATWAVE_SST_THRESHOLD = 28.0;
    private static final int HEATWAVE_MIN_DAYS = 5;
    private static final String TEMP_VAR = "thetao";
    private static final String CHL_VAR = "chl";

    @Scheduled(cron = "0 30 2 * * ?")
    public void assess() {
        LocalDate assessDate = systemConfigService.getSystemDate();
        log.info(">>>>>> 健康评估兜底检查: {}", assessDate);

        if (pipelineLock.isHealthAssessed(assessDate)) {
            log.info("健康评估已存在，跳过: {}", assessDate);
            return;
        }
        if (!pipelineLock.tryLock()) {
            log.info("流水线正在执行中，跳过: {}", assessDate);
            return;
        }
        try {
            run(assessDate);
            log.info("<<<<<< 健康评估完成: {}", assessDate);
        } catch (Exception e) {
            log.error("<<<<<< 健康评估失败: {}", assessDate, e);
        } finally {
            pipelineLock.unlock();
        }
    }

    public Map<String, Object> run(LocalDate assessDate) {
        List<HealthZone> zones = healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1));
        if (zones.isEmpty()) {
            return Map.of("message", "无活跃健康分区");
        }

        String dateStr = assessDate.toString();
        String prevDateStr = assessDate.minusDays(1).toString();
        Map<String, Object> result = new HashMap<>();
        int count = 0;

        for (HealthZone zone : zones) {
            try {
                // Temperature stats from observation_grid (thetao as SST proxy)
                Map<String, Object> tempStats = observationGridMapper.selectZoneStats(
                        TEMP_VAR, dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

                // CHL stats from observation_grid
                Map<String, Object> chlStats = observationGridMapper.selectZoneStats(
                        CHL_VAR, dateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

                if (tempStats == null || tempStats.get("avg_val") == null) {
                    log.debug("{} 无观测数据，跳过分区 {}", dateStr, zone.getZoneName());
                    continue;
                }

                double sstAvg = ((Number) tempStats.get("avg_val")).doubleValue();
                double sstMax = ((Number) tempStats.get("max_val")).doubleValue();
                double chlAvg = chlStats != null && chlStats.get("avg_val") != null
                        ? ((Number) chlStats.get("avg_val")).doubleValue() : 0;
                double chlMax = chlStats != null && chlStats.get("max_val") != null
                        ? ((Number) chlStats.get("max_val")).doubleValue() : 0;

                // anomaly: compare with all available observation_grid thetao for this zone
                Double baseline = observationGridMapper.selectZoneBaseline(
                        TEMP_VAR, assessDate.getMonthValue(),
                        zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
                double anomaly = baseline != null ? sstAvg - baseline : 0;

                // trend: compare with previous day observation
                Map<String, Object> prevTempStats = observationGridMapper.selectZoneStats(
                        TEMP_VAR, prevDateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());
                Map<String, Object> prevChlStats = observationGridMapper.selectZoneStats(
                        CHL_VAR, prevDateStr, zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat());

                String sstTrend = calcTrend(sstAvg, prevTempStats, SST_TREND_THRESHOLD);
                String chlTrend = calcTrend(chlAvg, prevChlStats, CHL_TREND_THRESHOLD);

                // heatwave detection from observation_grid thetao
                int[] hw = detectHeatwave(zone, dateStr);

                // grades
                String sstGrade = gradeSst(Math.abs(anomaly));
                String chlGrade = gradeChl(chlAvg);
                String hwGrade = hw[0] == 1 ? "bad" : "good";
                String overallGrade = worstGrade(sstGrade, chlGrade, hwGrade);
                String suggestions = buildAdvice(sstGrade, chlGrade, hwGrade);

                // upsert: delete existing then insert
                HealthRecord existing = healthRecordMapper.selectOne(
                        new LambdaQueryWrapper<HealthRecord>()
                                .eq(HealthRecord::getZoneId, zone.getId())
                                .eq(HealthRecord::getAssessDate, assessDate));
                if (existing != null) {
                    healthRecordMapper.deleteById(existing.getId());
                }

                HealthRecord record = new HealthRecord();
                record.setZoneId(zone.getId());
                record.setAssessDate(assessDate);
                record.setSstAvg(sstAvg);
                record.setSstMax(sstMax);
                record.setSstAnomaly(anomaly);
                record.setSstTrend(sstTrend);
                record.setChlAvg(chlAvg);
                record.setChlMax(chlMax);
                record.setChlTrend(chlTrend);
                record.setHeatwaveActive(hw[0]);
                record.setHeatwaveDays(hw[1]);
                record.setSstGrade(sstGrade);
                record.setChlGrade(chlGrade);
                record.setHeatwaveGrade(hwGrade);
                record.setOverallGrade(overallGrade);
                record.setSuggestions(suggestions);
                healthRecordMapper.insert(record);
                count++;
                log.info("健康评估 {} / {}: SST avg={} anomaly={} grade={}, CHL avg={} grade={}, overall={}",
                        zone.getZoneName(), assessDate, String.format("%.2f", sstAvg),
                        String.format("%.2f", anomaly), sstGrade, String.format("%.2f", chlAvg), chlGrade, overallGrade);
            } catch (Exception e) {
                log.error("健康评估失败: zone={}", zone.getZoneName(), e);
            }
        }

        result.put("message", "评估完成，写入 " + count + " 条记录");
        result.put("assessDate", dateStr);
        result.put("count", count);
        return result;
    }

    private String calcTrend(double currentAvg, Map<String, Object> prevStats, double threshold) {
        if (prevStats == null || prevStats.get("avg_val") == null) return "stable";
        double prevAvg = ((Number) prevStats.get("avg_val")).doubleValue();
        double diff = currentAvg - prevAvg;
        if (diff > threshold) return "rising";
        if (diff < -threshold) return "falling";
        return "stable";
    }

    private int[] detectHeatwave(HealthZone zone, String endDate) {
        String startDate = LocalDate.parse(endDate).minusDays(30).toString();
        List<Map<String, Object>> dailyAvgs = observationGridMapper.selectZoneDailyAvg(
                TEMP_VAR,
                zone.getMinLon(), zone.getMaxLon(), zone.getMinLat(), zone.getMaxLat(),
                startDate, endDate);

        int consecutive = 0;
        int maxConsecutive = 0;
        for (int i = 0; i < dailyAvgs.size(); i++) {
            double avg = ((Number) dailyAvgs.get(i).get("avg_val")).doubleValue();
            if (avg > HEATWAVE_SST_THRESHOLD) {
                consecutive++;
                maxConsecutive = Math.max(maxConsecutive, consecutive);
            } else {
                consecutive = 0;
            }
        }

        boolean active = consecutive >= HEATWAVE_MIN_DAYS;
        return new int[]{active ? 1 : 0, consecutive};
    }

    private String gradeSst(double absAnomaly) {
        if (absAnomaly > 2.5) return "bad";
        if (absAnomaly > 1.5) return "warn";
        if (absAnomaly > 0.5) return "fine";
        return "good";
    }

    private String gradeChl(double avg) {
        if (avg >= 5.0) return "bad";
        if (avg >= 3.0) return "warn";
        if (avg >= 2.0) return "fine";
        return "good";
    }

    private String worstGrade(String... grades) {
        List<String> order = List.of("good", "fine", "warn", "bad");
        String worst = "good";
        for (String g : grades) {
            if (order.indexOf(g) > order.indexOf(worst)) worst = g;
        }
        return worst;
    }

    private String buildAdvice(String sst, String chl, String hw) {
        List<String> advices = new ArrayList<>();
        if ("bad".equals(hw)) advices.add("远海渔业注意水温变化，评估对远洋作业的潜在影响");
        if ("bad".equals(sst) || "warn".equals(sst)) advices.add("关注未来 3 天 SST 变化趋势");
        if ("bad".equals(chl) || "warn".equals(chl)) advices.add("赤潮风险升高，建议加强监测");
        if (advices.isEmpty()) advices.add("各项指标正常，可正常作业");
        return String.join("；", advices);
    }
}
