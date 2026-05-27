package com.ocean.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String realName;

    private String phone;
}
