package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.config.ForecastConfig;
import com.ocean.dto.ForecastQueryDTO;
import com.ocean.dto.MapGridQueryDTO;
import com.ocean.entity.*;
import com.ocean.mapper.*;
import com.ocean.service.ForecastService;
import com.ocean.vo.DashboardVO;
import com.ocean.vo.ForecastVO;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class ForecastServiceImpl implements ForecastService {

    @Autowired private ForecastGridMapper forecastGridMapper;
    @Autowired private ModelMapper modelMapper;
    @Autowired private ModelVersionMapper modelVersionMapper;
    @Autowired private AlertEventMapper alertEventMapper;
    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private ForecastConfig forecastConfig;
    @Autowired private ObservationDataMapper observationDataMapper;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setModelCount(modelMapper.selectCount(null));
        vo.setRunningModelCount(modelVersionMapper.selectCount(
                new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getStatus, "RUNNING")));
        vo.setTodayRecordCount(forecastGridMapper.selectCount(
                new LambdaQueryWrapper<ForecastGrid>().eq(ForecastGrid::getForecastDate, systemConfigService.getSystemDate())));
        vo.setAlertCount(alertEventMapper.selectCount(
                new LambdaQueryWrapper<AlertEvent>().eq(AlertEvent::getStatus, "active")));
        vo.setLatestSstData(getLatestGridData("sst"));
        vo.setLatestChlData(getLatestGridData("chl"));
        return vo;
    }

    private List<Map<String, Object>> getLatestGridData(String variable) {
        List<Map<String, Object>> result = new ArrayList<>();
        LambdaQueryWrapper<ForecastGrid> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForecastGrid::getVariable, variable)
               .eq(ForecastGrid::getDepth, 0)
               .orderByDesc(ForecastGrid::getForecastDate)
               .last("LIMIT 5");
        List<ForecastGrid> list = forecastGridMapper.selectList(wrapper);
        for (ForecastGrid g : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("lat", g.getLat());
            m.put("lon", g.getLon());
            m.put("value", g.getValue());
            m.put("forecastDate", g.getForecastDate().toString());
            result.add(m);
        }
        return result;
    }

    @Override
    public IPage<ForecastVO> getRecordPage(ForecastQueryDTO dto) {
        Page<ForecastGrid> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<ForecastGrid> wrapper = new LambdaQueryWrapper<>();
        if (dto.getDataType() != null && !dto.getDataType().isEmpty())
            wrapper.eq(ForecastGrid::getVariable, dto.getDataType().toLowerCase());
        if (dto.getForecastDate() != null && !dto.getForecastDate().isEmpty())
            wrapper.eq(ForecastGrid::getForecastDate, dto.getForecastDate());
        wrapper.orderByDesc(ForecastGrid::getForecastDate);
        IPage<ForecastGrid> dataPage = forecastGridMapper.selectPage(page, wrapper);
        return dataPage.convert(this::toForecastVO);
    }

    private ForecastVO toForecastVO(ForecastGrid g) {
        ForecastVO vo = new ForecastVO();
        vo.setId(g.getId());
        vo.setDataType(g.getVariable().toUpperCase());
        vo.setForecastDate(g.getForecastDate());
        vo.setLongitude(BigDecimal.valueOf(g.getLon()));
        vo.setLatitude(BigDecimal.valueOf(g.getLat()));
        vo.setValue(g.getValue());
        vo.setUnit(g.getUnit());
        return vo;
    }

    @Override
    public List<Map<String, Object>> getSstTrend(Double lon, Double lat) {
        return forecastGridMapper.selectTrend("sst", lon, lat);
    }

    @Override
    public List<Map<String, Object>> getChlTrend(Double lon, Double lat) {
        return forecastGridMapper.selectTrend("chl", lon, lat);
    }

    @Override
    public List<Map<String, Object>> getLocations() {
        return forecastGridMapper.selectDistinctLocations();
    }

    @Override
    public List<Map<String, Object>> getMapGrid(MapGridQueryDTO dto) {
        return forecastGridMapper.selectMapGrid(
                dto.getDataType() != null ? dto.getDataType().toLowerCase() : "sst",
                dto.getForecastDate(),
                dto.getMinLon(), dto.getMaxLon(),
                dto.getMinLat(), dto.getMaxLat());
    }

    @Override
    public List<Map<String, Object>> getPointTrend(String dataType, Double lon, Double lat, String dateStart, String dateEnd) {
        return forecastGridMapper.selectPointTrend(dataType.toLowerCase(), lon, lat, dateStart, dateEnd);
    }

    @Override
    public List<Map<String, Object>> getDashboardTrend(String dataType, Integer days) {
        return forecastGridMapper.selectDashboardTrend(dataType.toLowerCase(), days);
    }

    @Override
    public List<Map<String, Object>> getChlProbability(String dateStart, String dateEnd, Double threshold) {
        return forecastGridMapper.selectChlProbability(dateStart, dateEnd, threshold != null ? threshold : 5.0);
    }

    @Override
    public List<Map<String, Object>> getSeaAreas() {
        List<Map<String, Object>> areas = new ArrayList<>();
        List<HealthZone> zones = healthZoneMapper.selectList(
                new LambdaQueryWrapper<HealthZone>().eq(HealthZone::getIsActive, 1));
        for (HealthZone zone : zones) {
            Map<String, Object> m = new HashMap<>();
            m.put("name", zone.getZoneName());
            m.put("minLon", zone.getMinLon());
            m.put("maxLon", zone.getMaxLon());
            m.put("minLat", zone.getMinLat());
            m.put("maxLat", zone.getMaxLat());
            areas.add(m);
        }
        return areas;
    }

    @Override
    public Map<String, Object> runForecast() {
        Map<String, Object> result = new HashMap<>();
        LocalDate systemDate = systemConfigService.getSystemDate();
        String dateStr = systemDate.toString();

        // 30-day window before system date
        LocalDate dataEnd = systemDate.minusDays(1);
        LocalDate dataStart = dataEnd.minusDays(30);
        String startStr = dataStart.toString();
        String endStr = dataEnd.toString();

        String pythonPath = forecastConfig.getPython().getPath();
        String scriptDir = forecastConfig.getPython().getScriptDir();
        String scriptPath = scriptDir + File.separator + "run_forecast.py";
        String outputPath = scriptDir + File.separator + "forecast_output.json";
        String csvPath = scriptDir + File.separator + "forecast_input.csv";

        log.info("Starting forecast: systemDate={}, dataRange=[{}, {}]", dateStr, startStr, endStr);

        try {
            // 1. Query observation_data and write temp CSV
            List<Map<String, Object>> rows = observationDataMapper.selectForecastInput(startStr, endStr);
            log.info("Queried {} rows from observation_data", rows.size());

            if (rows.size() < 1000) {
                result.put("success", false);
                result.put("message", "Insufficient data: only " + rows.size() + " rows in date range");
                return result;
            }

            try (PrintWriter pw = new PrintWriter(
                    new OutputStreamWriter(new FileOutputStream(csvPath), "UTF-8"))) {
                pw.println("time,depth,latitude,longitude,chl,thetao,so");
                for (Map<String, Object> row : rows) {
                    pw.printf("%s,%.4f,%.6f,%.6f,%s,%s,%s%n",
                            row.get("time"),
                            ((Number) row.get("depth")).doubleValue(),
                            ((Number) row.get("lat")).doubleValue(),
                            ((Number) row.get("lon")).doubleValue(),
                            nvl(row.get("chl")),
                            nvl(row.get("thetao")),
                            nvl(row.get("so")));
                }
            }
            log.info("Wrote temp CSV: {}", csvPath);

            // 2. Run Python script
            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath, scriptPath,
                    "--date", dateStr,
                    "--csv", csvPath,
                    "--output", outputPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder logBuf = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logBuf.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Python script failed (exit={}):\n{}", exitCode, logBuf);
                result.put("success", false);
                result.put("message", "Python script failed with exit code " + exitCode);
                result.put("log", logBuf.toString());
                return result;
            }
            log.info("Python output:\n{}", logBuf);

            // 3. Parse output JSON
            File outputFile = new File(outputPath);
            if (!outputFile.exists()) {
                result.put("success", false);
                result.put("message", "Output file not found: " + outputPath);
                return result;
            }

            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> predictions = mapper.readValue(
                    outputFile, new TypeReference<List<Map<String, Object>>>() {});

            if (predictions.isEmpty()) {
                result.put("success", false);
                result.put("message", "No predictions — check if data covers all grid points for 30 days");
                return result;
            }

            // 4. Write to forecast_grid
            Set<String> dateVars = new HashSet<>();
            List<ForecastGrid> grids = new ArrayList<>();

            for (Map<String, Object> pred : predictions) {
                ForecastGrid g = new ForecastGrid();
                Object mid = pred.get("model_id");
                g.setModelId(mid != null ? ((Number) mid).longValue() : 1L);
                g.setVersionId(1L);
                g.setVariable(String.valueOf(pred.get("variable")));
                g.setForecastDate(LocalDate.parse(String.valueOf(pred.get("forecast_date"))));
                g.setLat(((Number) pred.get("lat")).doubleValue());
                g.setLon(((Number) pred.get("lon")).doubleValue());
                g.setValue(((Number) pred.get("value")).doubleValue());
                g.setUnit(String.valueOf(pred.get("unit")));
                g.setDepth(0.0);
                grids.add(g);
                dateVars.add(g.getVariable() + "|" + g.getForecastDate());
            }

            for (String dv : dateVars) {
                String[] parts = dv.split("\\|");
                forecastGridMapper.delete(new LambdaQueryWrapper<ForecastGrid>()
                        .eq(ForecastGrid::getVariable, parts[0])
                        .eq(ForecastGrid::getForecastDate, LocalDate.parse(parts[1])));
            }

            for (ForecastGrid g : grids) {
                forecastGridMapper.insert(g);
            }

            log.info("Forecast complete: {} predictions inserted", grids.size());
            result.put("success", true);
            result.put("message", "Forecast complete");
            result.put("count", grids.size());
            result.put("systemDate", dateStr);
            result.put("dataRange", startStr + " ~ " + endStr);
            return result;

        } catch (Exception e) {
            log.error("Forecast run failed", e);
            result.put("success", false);
            result.put("message", e.getMessage());
            return result;
        } finally {
            try { new File(csvPath).delete(); } catch (Exception ignored) {}
        }
    }

    private static String nvl(Object v) {
        return v == null ? "" : v.toString();
    }
}
