package com.ocean.dto;

import lombok.Data;

@Data
public class AlertRuleSaveDTO {
    private String ruleName;
    private String variable;
    private String source;
    private String operator;
    private Double threshold;
    private String severity;
}
