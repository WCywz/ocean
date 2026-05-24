package com.ocean.service.impl;

import com.ocean.config.ForecastConfig;
import com.ocean.service.ObservationIngestService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ObservationIngestServiceImpl implements ObservationIngestService {

    @Autowired private ForecastConfig forecastConfig;
    @Autowired private SystemConfigService systemConfigService;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    /** Script that writes interpolated surface data to observation_grid. */
    private static final String GRID_SCRIPT = "../scripts/ingest_daily.py";

    /** Script that writes raw observation data to observation_data. */
    private static final String RAW_SCRIPT = "../scripts/ingest_raw_daily.py";

    private static final long TIMEOUT_SECONDS = 120;

    @Override
    public LocalDate ingestNextDay() {
        LocalDate nextDate = systemConfigService.getSystemDate().plusDays(1);
        ingestDate(nextDate);
        systemConfigService.advanceDay();
        log.info("Ingested data for {}, system date advanced", nextDate);
        return nextDate;
    }

    @Override
    public void ingestDate(LocalDate date) {
        String dataDir = forecastConfig.getDataDir();
        String dateStr = date.toString();

        // 1. Ingest raw observation_data from filtered raw CSVs
        runScript(RAW_SCRIPT, buildArgs(dateStr, "--data-dir", dataDir),
                "observation_data (raw)");

        // 2. Ingest observation_grid from interpolated unified-grid CSV
        runScript(GRID_SCRIPT, buildArgs(dateStr, "--csv",
                        dataDir + File.separator + "ocean_clean_post_2025.csv"),
                "observation_grid (grid)");
    }

    private List<String> buildArgs(String... args) {
        List<String> list = new ArrayList<>();
        for (String a : args) list.add(a);
        return list;
    }

    private void runScript(String scriptPath, List<String> extraArgs, String label) {
        try {
            String pythonPath = forecastConfig.getPython().getPath();
            String scriptDir = forecastConfig.getPython().getScriptDir();

            List<String> cmd = new ArrayList<>();
            cmd.add(pythonPath);
            cmd.add(scriptPath);
            cmd.addAll(extraArgs);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(new File(scriptDir));
            pb.redirectErrorStream(true);
            pb.environment().put("DB_USER", dbUsername);
            pb.environment().put("DB_PASSWORD", dbPassword);

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Ingest script timed out (" + label + ")");
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            if (process.exitValue() != 0) {
                throw new RuntimeException("Ingest script failed (" + label + "): " + output);
            }

            log.info("Ingest completed ({}): {}", label, output.toString().replace("\n", " | "));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to run ingest script (" + label + ")", e);
        }
    }
}
