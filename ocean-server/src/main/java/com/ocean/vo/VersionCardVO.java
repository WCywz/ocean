package com.ocean.vo;

import lombok.Data;

import java.util.List;

/**
 * 版本卡片视图对象（聚合版本信息 + 调度摘要列表）
 */
@Data
public class VersionCardVO {
    private Long modelId;
    private Long versionId;
    private String modelName;
    private String versionLabel;
    private String modelType;
    private String status;
    private List<ScheduleBrief> schedules;

    @Data
    public static class ScheduleBrief {
        private Long id;
        private String repetition;
        private Integer dayOfWeek;
        private String scheduleTime;
        private String scheduleDate;
    }
}
