package com.ocean.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AlertEventVO {

    private Long id;
    private Long versionId;
    private Long modelId;
    private String modelName;
    private String versionLabel;
    private String alertType;
    private String message;
    private Long runLogId;
    private Integer isRead;
    private LocalDateTime createTime;

    public String getTypeLabel() {
        if (alertType == null) return "";
        return switch (alertType) {
            case "EXECUTION_FAILED" -> "执行失败";
            case "CONSECUTIVE_FAILURES" -> "连续失败";
            case "EXECUTION_TIMEOUT" -> "执行超时";
            default -> alertType;
        };
    }
}
