package com.ocean.vo;

import lombok.Data;

/**
 * 登录返回信息
 */
@Data
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
}
