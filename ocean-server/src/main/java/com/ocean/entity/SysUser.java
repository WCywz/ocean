package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String realName;

    /** 角色: ADMIN-管理员, USER-普通用户 */
    private String role;

    /** 状态: 1-启用, 0-禁用 */
    private Integer status;

    /** 手机号（用于SMS通知） */
    private String phone;

    /** 头像URL路径 */
    private String avatarUrl;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
