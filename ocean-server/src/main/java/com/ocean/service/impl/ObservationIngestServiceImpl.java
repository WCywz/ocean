package com.ocean.service.impl;

import com.ocean.config.ForecastConfig;
import com.ocean.service.ObservationIngestService;
import com.ocean.service.SystemConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ObservationIngestServiceImpl implements ObservationIngestService {

    @Autowired private ForecastConfig forecastConfig;
    @Autowired private SystemConfigService systemConfigService;

    private static final String SCRIPT_PATH = "../scripts/ingest_daily.py";
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
        try {
            String pythonPath = forecastConfig.getPython().getPath();
            String scriptDir = forecastConfig.getPython().getScriptDir();

            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath, SCRIPT_PATH, date.toString());
            pb.directory(new java.io.File(scriptDir));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Ingest script timed out for " + date);
            }

            // Read output for logging
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            if (process.exitValue() != 0) {
                throw new RuntimeException("Ingest script failed for " + date + ": " + output);
            }

            log.info("Ingest {} completed: {}", date, output.toString().replace("\n", " | "));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to run ingest script for " + date, e);
        }
    }
}
