package com.ocean.dto;

import lombok.Data;

/**
 * 用户分页查询参数
 */
@Data
public class UserPageDTO {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    private String username;
    private String realName;
    private String role;
    private Integer status;
}
