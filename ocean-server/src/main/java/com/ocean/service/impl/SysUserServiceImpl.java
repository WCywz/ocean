package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.common.BusinessException;
import com.ocean.dto.LoginDTO;
import com.ocean.dto.PasswordChangeDTO;
import com.ocean.dto.ProfileUpdateDTO;
import com.ocean.dto.UserPageDTO;
import com.ocean.dto.UserSaveDTO;
import com.ocean.entity.SysUser;
import com.ocean.mapper.SysUserMapper;
import com.ocean.service.SysUserService;
import com.ocean.util.JwtUtil;
import com.ocean.vo.LoginVO;
import com.ocean.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 系统用户服务实现
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        // 缓存用户信息到Redis，24小时过期
        String cacheKey = "user:info:" + user.getId();
        redisTemplate.opsForValue().set(cacheKey, user, 24, TimeUnit.HOURS);

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setRole(user.getRole());
        vo.setAvatarUrl(user.getAvatarUrl());
        return vo;
    }

    @Override
    public IPage<UserVO> getUserPage(UserPageDTO dto) {
        Page<SysUser> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<SysUser> userPage = sysUserMapper.selectUserPage(page, dto);
        return userPage.convert(this::toVO);
    }

    @Override
    public UserVO getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toVO(user);
    }

    @Override
    public void register(UserSaveDTO dto) {
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        // 注册用户默认为普通用户角色
        user.setRole("USER");
        user.setStatus(1);
        sysUserMapper.insert(user);
    }

    @Override
    public void addUser(UserSaveDTO dto) {
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        sysUserMapper.insert(user);
    }

    @Override
    public void updateUser(UserSaveDTO dto) {
        SysUser user = sysUserMapper.selectById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 检查用户名是否被其他用户占用
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .ne(SysUser::getId, dto.getId())
        );
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        BeanUtils.copyProperties(dto, user, "password");
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        sysUserMapper.updateById(user);
        // 清除Redis缓存
        redisTemplate.delete("user:info:" + user.getId());
    }

    @Override
    public void deleteUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("ADMIN".equals(user.getRole())) {
            // 检查是否还有其他管理员
            Long adminCount = sysUserMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "ADMIN")
            );
            if (adminCount <= 1) {
                throw new BusinessException("系统必须保留至少一个管理员");
            }
        }
        sysUserMapper.deleteById(id);
        redisTemplate.delete("user:info:" + id);
    }

    @Override
    public void changePassword(Long userId, PasswordChangeDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次密码输入不一致");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    @Override
    public void updateAvatar(Long userId, String avatarUrl) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setAvatarUrl(avatarUrl);
        sysUserMapper.updateById(user);
    }

    @Override
    public void updateProfile(Long userId, ProfileUpdateDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        SysUser exist = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, dto.getUsername())
                        .ne(SysUser::getId, userId));
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setUsername(dto.getUsername());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        sysUserMapper.updateById(user);
        redisTemplate.delete("user:info:" + userId);
    }

    private UserVO toVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
