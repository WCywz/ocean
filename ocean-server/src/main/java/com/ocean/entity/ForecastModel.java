package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 预报模型配置实体
 */
@Data
@TableName("forecast_model")
public class ForecastModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联模型组ID */
    private Long groupId;

    /** 版本号标识（v1, v2, ...） */
    private String versionLabel;

    private String modelName;

    /** 模型类型: SST-海表温度, CHL-叶绿素浓度 */
    private String modelType;

    /** 模型参数配置（JSON格式） */
    private String paramsConfig;

    /** 运行周期Cron表达式 */
    private String cronExpression;

    /** 训练数据来源 */
    private String dataSource;

    /** 数据时间范围 */
    private String dataTimeRange;

    /** 变更说明（相对上一版本） */
    private String changeNote;

    /** 状态: RUNNING-运行中, STOPPED-已停止, ERROR-异常 */
    private String status;

    /** 最近运行时间 */
    private LocalDateTime lastRunTime;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
