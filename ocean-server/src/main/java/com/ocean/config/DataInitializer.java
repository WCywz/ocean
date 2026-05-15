package com.ocean.config;

import com.ocean.entity.SysUser;
import com.ocean.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化 - 创建默认管理员和普通用户
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 初始化管理员账号
        if (sysUserMapper.selectCount(null) == 0) {
            SysUser admin = new SysUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRealName("系统管理员");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            sysUserMapper.insert(admin);

            SysUser user = new SysUser();
            user.setUsername("user");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setRealName("普通用户");
            user.setRole("USER");
            user.setStatus(1);
            sysUserMapper.insert(user);

            System.out.println(">>> 默认用户初始化完成: admin/admin123, user/user123");
        }
    }
}
