package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.LoginDTO;
import com.ocean.dto.UserPageDTO;
import com.ocean.dto.UserSaveDTO;
import com.ocean.service.SysUserService;
import com.ocean.vo.LoginVO;
import com.ocean.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/api/user")
public class  UserController {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO dto) {
        LoginVO vo = sysUserService.login(dto);
        return Result.success("登录成功", vo);
    }

    /**
     * 用户注册（公开接口）
     */
    @PostMapping("/register")
    public Result<?> register(@Validated @RequestBody UserSaveDTO dto) {
        sysUserService.register(dto);
        return Result.success("注册成功，请登录");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/current")
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserVO vo = sysUserService.getUserById(userId);
        return Result.success(vo);
    }

    /**
     * 分页查询用户（管理员）
     */
    @GetMapping("/page")
    public Result<IPage<UserVO>> getUserPage(@Validated UserPageDTO dto) {
        IPage<UserVO> page = sysUserService.getUserPage(dto);
        return Result.success(page);
    }

    /**
     * 根据ID查询用户（管理员）
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO vo = sysUserService.getUserById(id);
        return Result.success(vo);
    }

    /**
     * 新增用户（管理员）
     */
    @PostMapping
    public Result<?> addUser(@Validated @RequestBody UserSaveDTO dto) {
        sysUserService.addUser(dto);
        return Result.success("用户创建成功");
    }

    /**
     * 修改用户（管理员）
     */
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @Validated @RequestBody UserSaveDTO dto) {
        dto.setId(id);
        sysUserService.updateUser(dto);
        return Result.success("用户更新成功");
    }

    /**
     * 删除用户（管理员）
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return Result.success("用户删除成功");
    }
}
