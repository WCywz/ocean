package com.ocean.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息返回
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String realName;
    private String role;
    private String phone;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
