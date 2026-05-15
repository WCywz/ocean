package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.LoginDTO;
import com.ocean.dto.UserPageDTO;
import com.ocean.dto.UserSaveDTO;
import com.ocean.entity.SysUser;
import com.ocean.vo.LoginVO;
import com.ocean.vo.UserVO;

/**
 * 系统用户服务接口
 */
public interface SysUserService {

    /** 登录 */
    LoginVO login(LoginDTO dto);

    /** 分页查询用户 */
    IPage<UserVO> getUserPage(UserPageDTO dto);

    /** 根据ID查询用户 */
    UserVO getUserById(Long id);

    /** 用户注册 */
    void register(UserSaveDTO dto);

    /** 新增用户（管理员） */
    void addUser(UserSaveDTO dto);

    /** 修改用户 */
    void updateUser(UserSaveDTO dto);

    /** 删除用户 */
    void deleteUser(Long id);
}
