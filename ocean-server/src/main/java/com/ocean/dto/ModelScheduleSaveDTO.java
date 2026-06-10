package com.ocean.dto;

import lombok.Data;

/**
 * 模型调度保存/更新请求 DTO
 */
@Data
public class ModelScheduleSaveDTO {
    private Long id;
    private Long versionId;
    private String scheduleLabel;
    private String repetition;
    private Integer dayOfWeek;
    private String scheduleTime;
    private String scheduleDate;
}
