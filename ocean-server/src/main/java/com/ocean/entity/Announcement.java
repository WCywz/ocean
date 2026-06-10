package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
<<<<<<<< HEAD:ocean-server/src/main/java/com/ocean/entity/UserSetting.java
@TableName("user_setting")
public class UserSetting {
========
@TableName("announcement")
public class Announcement {
>>>>>>>> feature/model-integration:ocean-server/src/main/java/com/ocean/entity/Announcement.java

    @TableId(type = IdType.AUTO)
    private Long id;

<<<<<<<< HEAD:ocean-server/src/main/java/com/ocean/entity/UserSetting.java
    private Long userId;

    private String settingKey;

    private String settingValue;
========
    private String title;

    private String content;
>>>>>>>> feature/model-integration:ocean-server/src/main/java/com/ocean/entity/Announcement.java

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
