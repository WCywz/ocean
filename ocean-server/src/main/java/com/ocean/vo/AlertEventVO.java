package com.ocean.vo;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AlertEventVO {
    private Long id;
    private Long zoneId;
    private String zoneName;
    private Long ruleId;
    private String ruleName;
    private String variable;
    private String source;
    private LocalDate alertDate;
    private Double maxValue;
    private Double avgValue;
    private Double threshold;
    private Integer stationCount;
    private String severity;
    private String status;
    private String message;
    private LocalDateTime createTime;
}
