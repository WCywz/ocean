package com.ocean.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "forecast")
public class ForecastConfig {
    private String dataDir = "./data";
    private PythonConfig python = new PythonConfig();

    @Data
    public static class PythonConfig {
        private String path = "python3";
        private String scriptDir = "ocean-model";
    }
}
