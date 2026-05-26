package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_setting")
public class UserSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String settingKey;

    private String settingValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
