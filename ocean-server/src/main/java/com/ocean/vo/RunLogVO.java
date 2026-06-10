package com.ocean.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RunLogVO {

    private Long id;
    private Long versionId;
    private Long modelId;
    private String modelName;
    private String versionLabel;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String status;
    private String errorMessage;
    private String outputSummary;
    private String logText;
    private LocalDateTime createTime;

    public String getDurationDisplay() {
        if (durationMs == null || durationMs == 0) return "-";
        long seconds = durationMs / 1000;
        if (seconds < 60) return seconds + "秒";
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + "分" + seconds + "秒";
    }
}
