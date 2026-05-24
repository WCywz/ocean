package com.ocean.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 用户新增/修改参数
 */
@Data
public class UserSaveDTO {

    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 新增时必填，修改时可不填 */
    private String password;

    private String realName;

    @NotNull(message = "角色不能为空")
    private String role;

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String phone;
}
