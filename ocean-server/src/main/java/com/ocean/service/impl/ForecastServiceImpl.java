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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ForecastServiceImpl implements ForecastService {

    private static final long PREPARE_TIMEOUT_SECONDS = 120;
    private static final long INFERENCE_TIMEOUT_SECONDS = 300;

    @Autowired private ForecastGridMapper forecastGridMapper;
    @Autowired private ModelMapper modelMapper;
    @Autowired private ModelVersionMapper modelVersionMapper;
    @Autowired private HealthZoneMapper healthZoneMapper;
    @Autowired private SystemConfigService systemConfigService;
    @Autowired private ForecastConfig forecastConfig;
    @Autowired private ObservationDataMapper observationDataMapper;
    @Autowired private MonitoringStationMapper monitoringStationMapper;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setModelCount(modelMapper.selectCount(null));
        vo.setRunningModelCount(modelVersionMapper.selectCount(
                new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getStatus, "RUNNING")));
        vo.setTodayRecordCount(forecastGridMapper.selectCount(
                new LambdaQueryWrapper<ForecastGrid>().eq(ForecastGrid::getForecastDate, systemConfigService.getSystemDate())));
        vo.setLatestSstData(getStationObsData("sst"));
        vo.setLatestChlData(getStationObsData("chl"));
        return vo;
    }

    private List<Map<String, Object>> getStationObsData(String variable) {
        String dbVariable = variable.equals("sst") ? "thetao" : "chl";

        List<MonitoringStation> stations = monitoringStationMapper.selectList(
                new LambdaQueryWrapper<MonitoringStation>().eq(MonitoringStation::getIsActive, 1));

        List<Map<String, Object>> result = new ArrayList<>();
        for (MonitoringStation station : stations) {
            Map<String, Object> m = new HashMap<>();
            m.put("locationName", station.getStationName());
            m.put("lat", station.getLat());
            m.put("lon", station.getLon());

            Map<String, Object> row = observationDataMapper.selectLatestStationObsByPoint(
                    dbVariable, station.getLat(), station.getLon());
            if (row != null) {
                m.put("value", row.get("value"));
                m.put("forecastDate", row.get("obsDate").toString());
            } else {
                m.put("value", null);
                m.put("forecastDate", systemConfigService.getSystemDate().toString());
            }
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
        LocalDate systemDate = systemConfigService.getSystemDate();
        String start = systemDate.plusDays(1).toString();
        String end = systemDate.plusDays(7).toString();
        return forecastGridMapper.selectPointTrend(dataType.toLowerCase(), lon, lat, start, end);
    }

    @Override
    public List<Map<String, Object>> getDashboardTrend(String dataType, Integer days) {
        String startDate = systemConfigService.getSystemDate().plusDays(1).toString();
        String endDate = systemConfigService.getSystemDate().plusDays(days + 1).toString();

        // Use map center point (29.8, 123.5) to find nearest grid point
        // Filter by fromDate to only consider points with current forecast data
        Map<String, Object> nearest = forecastGridMapper.selectNearestPoint(29.8, 123.5, startDate);
        Double ptLat = ((Number) nearest.get("lat")).doubleValue();
        Double ptLon = ((Number) nearest.get("lon")).doubleValue();
        List<Map<String, Object>> dataPoints = forecastGridMapper.selectDashboardTrend(
                dataType.toLowerCase(), ptLat, ptLon, startDate, endDate);

        Map<String, Object> series = new HashMap<>();
        series.put("locationName", String.format("(%.2f°N, %.2f°E)", ptLat, ptLon));
        series.put("longitude", ptLon);
        series.put("latitude", ptLat);
        series.put("dataPoints", dataPoints);
        return List.of(series);
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
        return runForecast(1L, 1L, null);
    }

    @Override
    public Map<String, Object> runForecast(Long versionId, Long modelId, String modelType) {
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

        // Map model type to forecast variable name
        String targetVariable = modelTypeToVariable(modelType);
        log.info("Starting forecast: systemDate={}, dataRange=[{}, {}], modelType={}, targetVar={}",
                dateStr, startStr, endStr, modelType, targetVariable);

        try {
            // 1. Generate forecast input CSV from interpolated CSVs
            String prepareScript = scriptDir + File.separator + "prepare_forecast_input.py";

            ProcessBuilder pbPrepare = new ProcessBuilder(
                    pythonPath, prepareScript,
                    "--start", startStr,
                    "--end", endStr,
                    "--output", csvPath,
                    "--data-dir", forecastConfig.getDataDir());
            pbPrepare.redirectErrorStream(true);
            Process procPrepare = pbPrepare.start();

            StringBuilder prepareLog = new StringBuilder();
            Thread prepareReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(procPrepare.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        prepareLog.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            }, "forecast-prepare-reader");
            prepareReader.start();

            boolean prepareFinished = procPrepare.waitFor(PREPARE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!prepareFinished) {
                procPrepare.destroyForcibly();
                prepareReader.interrupt();
                log.error("Prepare script timed out after {}s", PREPARE_TIMEOUT_SECONDS);
                result.put("success", false);
                result.put("message", "Prepare script timed out");
                return result;
            }
            try { prepareReader.join(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            int prepareExit = procPrepare.exitValue();
            if (prepareExit != 0) {
                log.error("Prepare script failed (exit={}):\n{}", prepareExit, prepareLog);
                result.put("success", false);
                result.put("message", "Prepare script failed with exit code " + prepareExit);
                result.put("log", prepareLog.toString());
                return result;
            }
            log.info("Prepare input CSV: {}", prepareLog.toString().replace("\n", " | "));

            File csvFile = new File(csvPath);
            if (!csvFile.exists() || csvFile.length() == 0) {
                result.put("success", false);
                result.put("message", "No data in date range: " + startStr + " ~ " + endStr);
                return result;
            }

            // 2. Run model inference
            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath, scriptPath,
                    "--date", dateStr,
                    "--csv", csvPath,
                    "--output", outputPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder logBuf = new StringBuilder();
            Thread inferenceReader = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logBuf.append(line).append("\n");
                    }
                } catch (IOException ignored) {}
            }, "forecast-inference-reader");
            inferenceReader.start();

            boolean finished = process.waitFor(INFERENCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                inferenceReader.interrupt();
                log.error("Model inference timed out after {}s", INFERENCE_TIMEOUT_SECONDS);
                result.put("success", false);
                result.put("message", "Model inference timed out");
                return result;
            }
            try { inferenceReader.join(5000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            int exitCode = process.exitValue();
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

            // 4. Filter by target variable and write to forecast_grid
            Set<String> dateVars = new HashSet<>();
            List<ForecastGrid> grids = new ArrayList<>();

            for (Map<String, Object> pred : predictions) {
                String variable = String.valueOf(pred.get("variable"));
                if (targetVariable != null && !targetVariable.equalsIgnoreCase(variable)) {
                    continue;
                }
                ForecastGrid g = new ForecastGrid();
                g.setModelId(modelId != null ? modelId : 1L);
                g.setVersionId(versionId != null ? versionId : 1L);
                g.setVariable(variable);
                g.setForecastDate(LocalDate.parse(String.valueOf(pred.get("forecast_date"))));
                g.setLat(((Number) pred.get("lat")).doubleValue());
                g.setLon(((Number) pred.get("lon")).doubleValue());
                g.setValue(((Number) pred.get("value")).doubleValue());
                g.setUnit(String.valueOf(pred.get("unit")));
                g.setDepth(0.0);
                grids.add(g);
                dateVars.add(g.getVariable() + "|" + g.getForecastDate());
            }

            if (grids.isEmpty()) {
                String msg = targetVariable != null
                        ? "No predictions matched variable: " + targetVariable
                        : "No predictions";
                log.warn(msg);
                result.put("success", false);
                result.put("message", msg);
                return result;
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

            log.info("Forecast complete: {} predictions inserted (filtered to {})", grids.size(), targetVariable);
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
            try {
                File csvFile = new File(csvPath);
                if (csvFile.exists() && !csvFile.delete()) {
                    log.warn("无法删除临时CSV文件: {}", csvPath);
                }
            } catch (Exception e) {
                log.warn("删除临时CSV文件异常: {}", csvPath, e);
            }
        }
    }

    private String modelTypeToVariable(String modelType) {
        if (modelType == null) return null;
        return switch (modelType.toUpperCase()) {
            case "SST" -> "sst";
            case "CHL" -> "chl";
            case "SALINITY" -> "so";
            default -> null;
        };
    }

}
