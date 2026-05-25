# 用户体系完善 实施方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补全用户体系：个人中心（信息编辑 + 头像 + 修改密码）、通知偏好设置、密钥管理、角色权限修复、前端版本号展示。

**Architecture:** 新建 ProfileController 处理个人中心 API，新建 RoleInterceptor 拦截 `/api/user/**` 做 ADMIN 校验，新建 user_setting / user_credential 两张表，SysUser 增加 avatar_url 字段，前端新建 ProfileView.vue，导航栏增加头像下拉菜单。

**Tech Stack:** Java 21, Spring Boot 3.4.1, MyBatis-Plus 3.5.7, MySQL, Vue 3, Vite 5, Element Plus, Pinia, Axios

---

## 文件结构

```
新建:
  database/migration/002-user-system-enhance.sql
  ocean-server/.../entity/UserSetting.java
  ocean-server/.../entity/UserCredential.java
  ocean-server/.../mapper/UserSettingMapper.java
  ocean-server/.../mapper/UserCredentialMapper.java
  ocean-server/.../service/UserSettingService.java
  ocean-server/.../service/impl/UserSettingServiceImpl.java
  ocean-server/.../service/UserCredentialService.java
  ocean-server/.../service/impl/UserCredentialServiceImpl.java
  ocean-server/.../dto/ProfileUpdateDTO.java
  ocean-server/.../dto/PasswordChangeDTO.java
  ocean-server/.../dto/SettingsUpdateDTO.java
  ocean-server/.../dto/CredentialSaveDTO.java
  ocean-server/.../util/AesUtil.java
  ocean-server/.../controller/ProfileController.java
  ocean-server/.../config/RoleInterceptor.java
  ocean-web/src/api/profile.js
  ocean-web/src/views/profile/ProfileView.vue

修改:
  ocean-server/.../entity/SysUser.java                 — 加 avatarUrl
  ocean-server/.../vo/UserVO.java                      — 加 avatarUrl
  ocean-server/.../vo/LoginVO.java                     — 加 avatarUrl
  ocean-server/.../config/WebMvcConfig.java            — 注册 RoleInterceptor + 静态资源映射
  ocean-server/.../task/HealthSmsTask.java             — 检查 sms_enabled
  ocean-web/src/store/user.js                          — 加 avatarUrl
  ocean-web/src/router/index.js                        — 加 /app/profile 路由
  ocean-web/src/layout/MainLayout.vue                  — 头像下拉菜单 + footer 版本号
  ocean-web/vite.config.js                             — 注入版本号
```

---

### Task 1: 数据库迁移

**Files:**
- Create: `database/migration/002-user-system-enhance.sql`

- [ ] **Step 1: 编写迁移 SQL**

```sql
-- 用户头像列
ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像URL路径';

-- 用户偏好设置表
CREATE TABLE user_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    setting_key VARCHAR(50) NOT NULL COMMENT '设置键',
    setting_value VARCHAR(500) NOT NULL DEFAULT '' COMMENT '设置值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    CONSTRAINT fk_user_setting_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) COMMENT '用户偏好设置表';

-- 用户密钥表（AES加密存储）
CREATE TABLE user_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    credential_key VARCHAR(50) NOT NULL COMMENT '密钥类型',
    credential_value VARCHAR(1000) NOT NULL COMMENT '密钥值(AES加密)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_credential (user_id, credential_key),
    CONSTRAINT fk_user_credential_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) COMMENT '用户密钥表';
```

- [ ] **Step 2: 执行迁移**

```bash
mysql -u root -pyour_password ocean_forecast < database/migration/002-user-system-enhance.sql
```

- [ ] **Step 3: 验证**

```sql
DESC sys_user;          -- avatar_url 列存在
SHOW TABLES LIKE 'user_%';  -- user_setting, user_credential 存在
SHOW CREATE TABLE user_setting;
```

---

### Task 2: 后端实体 — UserSetting 和 UserCredential

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/entity/UserSetting.java`
- Create: `ocean-server/src/main/java/com/ocean/entity/UserCredential.java`

- [ ] **Step 1: 创建 UserSetting 实体**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_setting")
public class UserSetting {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String settingKey;

    private String settingValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 UserCredential 实体**

```java
package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_credential")
public class UserCredential {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String credentialKey;

    private String credentialValue;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

---

### Task 3: 后端 Mapper

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/mapper/UserSettingMapper.java`
- Create: `ocean-server/src/main/java/com/ocean/mapper/UserCredentialMapper.java`

- [ ] **Step 1: 创建 UserSettingMapper**

```java
package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.UserSetting;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserSettingMapper extends BaseMapper<UserSetting> {
}
```

- [ ] **Step 2: 创建 UserCredentialMapper**

```java
package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ocean.entity.UserCredential;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserCredentialMapper extends BaseMapper<UserCredential> {
}
```

---

### Task 4: AES 加密工具

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/util/AesUtil.java`

- [ ] **Step 1: 创建 AesUtil**

```java
package com.ocean.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH = 16;

    public static String encrypt(String plainText, String secret) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec keySpec = new SecretKeySpec(
                    padKey(secret), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, combined, IV_LENGTH, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encrypt failed", e);
        }
    }

    public static String decrypt(String cipherText, String secret) {
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(
                    padKey(secret), "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decrypt failed", e);
        }
    }

    private static byte[] padKey(String secret) {
        byte[] key = new byte[16];
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, key, 0,
                Math.min(secretBytes.length, 16));
        return key;
    }

    public static String mask(String value) {
        if (value == null || value.length() <= 4) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 2);
    }
}
```

- [ ] **Step 2: application.yml.example 加配置项**

在 `application.yml.example` 的 `jwt:` 段后增加：

```yaml
credential:
  encrypt:
    secret: your_aes_encrypt_secret_key

upload:
  avatar:
    dir: /data/ocean/uploads/avatars/
```

---

### Task 5: 后端 DTO

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/dto/ProfileUpdateDTO.java`
- Create: `ocean-server/src/main/java/com/ocean/dto/PasswordChangeDTO.java`
- Create: `ocean-server/src/main/java/com/ocean/dto/SettingsUpdateDTO.java`
- Create: `ocean-server/src/main/java/com/ocean/dto/CredentialSaveDTO.java`

- [ ] **Step 1: 创建 ProfileUpdateDTO**

```java
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
```

- [ ] **Step 2: 创建 PasswordChangeDTO**

```java
package com.ocean.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordChangeDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
```

- [ ] **Step 3: 创建 SettingsUpdateDTO**

```java
package com.ocean.dto;

import lombok.Data;
import java.util.Map;

@Data
public class SettingsUpdateDTO {
    private Map<String, String> settings;
}
```

- [ ] **Step 4: 创建 CredentialSaveDTO**

```java
package com.ocean.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CredentialSaveDTO {

    @NotBlank(message = "密钥类型不能为空")
    private String credentialKey;

    @NotBlank(message = "密钥值不能为空")
    private String credentialValue;
}
```

---

### Task 6: 后端 Service — UserSettingService

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/UserSettingService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/UserSettingServiceImpl.java`

- [ ] **Step 1: 创建接口**

```java
package com.ocean.service;

import com.ocean.entity.UserSetting;

import java.util.List;
import java.util.Map;

public interface UserSettingService {

    /** 获取用户所有设置项（含默认值） */
    Map<String, String> getUserSettings(Long userId);

    /** 批量更新设置（合并更新） */
    void updateSettings(Long userId, Map<String, String> settings);

    /** 根据userId列出设置记录 */
    List<UserSetting> listByUserId(Long userId);
}
```

- [ ] **Step 2: 创建实现**

```java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ocean.entity.UserSetting;
import com.ocean.mapper.UserSettingMapper;
import com.ocean.service.UserSettingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserSettingServiceImpl implements UserSettingService {

    @Autowired
    private UserSettingMapper userSettingMapper;

    private static final Map<String, String> DEFAULTS = Map.of(
            "sms_enabled", "true",
            "push_enabled", "true"
    );

    @Override
    public Map<String, String> getUserSettings(Long userId) {
        List<UserSetting> records = listByUserId(userId);
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        for (UserSetting r : records) {
            result.put(r.getSettingKey(), r.getSettingValue());
        }
        return result;
    }

    @Override
    public void updateSettings(Long userId, Map<String, String> settings) {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            LambdaQueryWrapper<UserSetting> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserSetting::getUserId, userId)
                   .eq(UserSetting::getSettingKey, entry.getKey());
            UserSetting existing = userSettingMapper.selectOne(wrapper);
            if (existing != null) {
                existing.setSettingValue(entry.getValue());
                userSettingMapper.updateById(existing);
            } else {
                UserSetting us = new UserSetting();
                us.setUserId(userId);
                us.setSettingKey(entry.getKey());
                us.setSettingValue(entry.getValue());
                userSettingMapper.insert(us);
            }
        }
    }

    @Override
    public List<UserSetting> listByUserId(Long userId) {
        return userSettingMapper.selectList(
                new LambdaQueryWrapper<UserSetting>()
                        .eq(UserSetting::getUserId, userId));
    }
}
```

---

### Task 7: 后端 Service — UserCredentialService

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/service/UserCredentialService.java`
- Create: `ocean-server/src/main/java/com/ocean/service/impl/UserCredentialServiceImpl.java`

- [ ] **Step 1: 创建接口**

```java
package com.ocean.service;

import com.ocean.entity.UserCredential;
import com.ocean.dto.CredentialSaveDTO;

import java.util.List;
import java.util.Map;

public interface UserCredentialService {

    /** 获取用户密钥列表（value 解密后脱敏） */
    List<Map<String, Object>> listCredentials(Long userId);

    /** 添加/更新密钥 */
    void saveCredential(Long userId, CredentialSaveDTO dto);

    /** 删除密钥 */
    void deleteCredential(Long userId, Long credentialId);

    /** 获取密钥原文（内部调用，不暴露给前端） */
    String getCredentialValue(Long userId, String credentialKey);
}
```

- [ ] **Step 2: 创建实现**

```java
package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.dto.CredentialSaveDTO;
import com.ocean.entity.UserCredential;
import com.ocean.mapper.UserCredentialMapper;
import com.ocean.service.UserCredentialService;
import com.ocean.util.AesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserCredentialServiceImpl implements UserCredentialService {

    @Autowired
    private UserCredentialMapper userCredentialMapper;

    @Value("${credential.encrypt.secret}")
    private String encryptSecret;

    @Override
    public List<Map<String, Object>> listCredentials(Long userId) {
        List<UserCredential> records = userCredentialMapper.selectList(
                new LambdaQueryWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, userId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCredential r : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("credentialKey", r.getCredentialKey());
            String decrypted = AesUtil.decrypt(r.getCredentialValue(), encryptSecret);
            item.put("credentialValue", AesUtil.mask(decrypted));
            result.add(item);
        }
        return result;
    }

    @Override
    public void saveCredential(Long userId, CredentialSaveDTO dto) {
        LambdaQueryWrapper<UserCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCredential::getUserId, userId)
               .eq(UserCredential::getCredentialKey, dto.getCredentialKey());
        UserCredential existing = userCredentialMapper.selectOne(wrapper);

        String encrypted = AesUtil.encrypt(dto.getCredentialValue(), encryptSecret);

        if (existing != null) {
            existing.setCredentialValue(encrypted);
            userCredentialMapper.updateById(existing);
        } else {
            UserCredential uc = new UserCredential();
            uc.setUserId(userId);
            uc.setCredentialKey(dto.getCredentialKey());
            uc.setCredentialValue(encrypted);
            userCredentialMapper.insert(uc);
        }
    }

    @Override
    public void deleteCredential(Long userId, Long credentialId) {
        UserCredential uc = userCredentialMapper.selectById(credentialId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new RuntimeException("密钥不存在或无权操作");
        }
        userCredentialMapper.deleteById(credentialId);
    }

    @Override
    public String getCredentialValue(Long userId, String credentialKey) {
        LambdaQueryWrapper<UserCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCredential::getUserId, userId)
               .eq(UserCredential::getCredentialKey, credentialKey);
        UserCredential uc = userCredentialMapper.selectOne(wrapper);
        if (uc == null) return null;
        return AesUtil.decrypt(uc.getCredentialValue(), encryptSecret);
    }
}
```

---

### Task 8: 后端 ProfileController

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/controller/ProfileController.java`

- [ ] **Step 1: 创建 ProfileController**

```java
package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.*;
import com.ocean.service.SysUserService;
import com.ocean.service.UserSettingService;
import com.ocean.service.UserCredentialService;
import com.ocean.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserCredentialService userCredentialService;

    @Value("${upload.avatar.dir:/data/ocean/uploads/avatars/}")
    private String avatarDir;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    /** 获取当前用户信息 */
    @GetMapping
    public Result<UserVO> getProfile(HttpServletRequest request) {
        return Result.success(sysUserService.getUserById(getUserId(request)));
    }

    /** 编辑个人信息 */
    @PutMapping
    public Result<?> updateProfile(HttpServletRequest request,
                                   @Validated @RequestBody ProfileUpdateDTO dto) {
        UserSaveDTO saveDto = new UserSaveDTO();
        saveDto.setId(getUserId(request));
        saveDto.setUsername(dto.getUsername());
        saveDto.setRealName(dto.getRealName());
        saveDto.setPhone(dto.getPhone());
        sysUserService.updateUser(saveDto);
        return Result.success("更新成功");
    }

    /** 修改密码 */
    @PutMapping("/password")
    public Result<?> changePassword(HttpServletRequest request,
                                    @Validated @RequestBody PasswordChangeDTO dto) {
        sysUserService.changePassword(getUserId(request), dto);
        return Result.success("密码修改成功");
    }

    /** 上传头像 */
    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(HttpServletRequest request,
                                                    @RequestParam("file") MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            return Result.error("文件大小不能超过2MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            return Result.error("无效的文件");
        }
        String ext = original.substring(original.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error("只支持 jpg、png、gif 格式");
        }

        Long userId = getUserId(request);
        File dir = new File(avatarDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = userId + "_" + System.currentTimeMillis() + "." + ext;
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
            return Result.error("头像上传失败");
        }

        String avatarUrl = "/uploads/avatars/" + fileName;
        sysUserService.updateAvatar(userId, avatarUrl);

        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);
        return Result.success("上传成功", data);
    }

    /** 获取设置 */
    @GetMapping("/settings")
    public Result<Map<String, String>> getSettings(HttpServletRequest request) {
        return Result.success(userSettingService.getUserSettings(getUserId(request)));
    }

    /** 批量更新设置 */
    @PutMapping("/settings")
    public Result<?> updateSettings(HttpServletRequest request,
                                    @Validated @RequestBody SettingsUpdateDTO dto) {
        userSettingService.updateSettings(getUserId(request), dto.getSettings());
        return Result.success("设置已更新");
    }

    /** 获取密钥列表 */
    @GetMapping("/credentials")
    public Result<List<Map<String, Object>>> getCredentials(HttpServletRequest request) {
        return Result.success(userCredentialService.listCredentials(getUserId(request)));
    }

    /** 添加/更新密钥 */
    @PostMapping("/credentials")
    public Result<?> saveCredential(HttpServletRequest request,
                                    @Validated @RequestBody CredentialSaveDTO dto) {
        userCredentialService.saveCredential(getUserId(request), dto);
        return Result.success("密钥保存成功");
    }

    /** 删除密钥 */
    @DeleteMapping("/credentials/{id}")
    public Result<?> deleteCredential(HttpServletRequest request, @PathVariable Long id) {
        userCredentialService.deleteCredential(getUserId(request), id);
        return Result.success("密钥已删除");
    }
}
```

---

### Task 9: 后端 SysUserService 补充方法

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/service/SysUserService.java`
- Modify: `ocean-server/src/main/java/com/ocean/service/impl/SysUserServiceImpl.java`

- [ ] **Step 1: SysUserService 接口新增方法声明**

在现有接口末尾增加：

```java
/** 修改密码 */
void changePassword(Long userId, PasswordChangeDTO dto);

/** 更新头像 */
void updateAvatar(Long userId, String avatarUrl);
```

- [ ] **Step 2: 检查 SysUserServiceImpl 现有实现**

先阅读 `ocean-server/src/main/java/com/ocean/service/impl/SysUserServiceImpl.java`，确认现有 `updateUser` 逻辑，然后新增两个方法：

```java
@Override
public void changePassword(Long userId, PasswordChangeDTO dto) {
    if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
        throw new RuntimeException("两次密码输入不一致");
    }
    SysUser user = sysUserMapper.selectById(userId);
    if (user == null) {
        throw new RuntimeException("用户不存在");
    }
    if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
        throw new RuntimeException("旧密码错误");
    }
    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    sysUserMapper.updateById(user);
}

@Override
public void updateAvatar(Long userId, String avatarUrl) {
    SysUser user = sysUserMapper.selectById(userId);
    if (user == null) {
        throw new RuntimeException("用户不存在");
    }
    user.setAvatarUrl(avatarUrl);
    sysUserMapper.updateById(user);
}
```

---

### Task 10: 修改已有实体和 VO

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/entity/SysUser.java`
- Modify: `ocean-server/src/main/java/com/ocean/vo/UserVO.java`
- Modify: `ocean-server/src/main/java/com/ocean/vo/LoginVO.java`

- [ ] **Step 1: SysUser 加 avatarUrl**

在 `SysUser.java` 的 `phone` 字段后增加：

```java
/** 头像URL路径 */
private String avatarUrl;
```

- [ ] **Step 2: UserVO 加 avatarUrl**

在 `UserVO.java` 的 `phone` 字段后增加：

```java
private String avatarUrl;
```

- [ ] **Step 3: LoginVO 加 avatarUrl**

在 `LoginVO.java` 的 `role` 字段后增加：

```java
private String avatarUrl;
```

---

### Task 11: 后端 RoleInterceptor + WebMvcConfig

**Files:**
- Create: `ocean-server/src/main/java/com/ocean/config/RoleInterceptor.java`
- Modify: `ocean-server/src/main/java/com/ocean/config/WebMvcConfig.java`

- [ ] **Step 1: 创建 RoleInterceptor**

```java
package com.ocean.config;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"权限不足，仅管理员可操作\"}");
            return false;
        }
        return true;
    }
}
```

- [ ] **Step 2: 修改 WebMvcConfig**

```java
package com.ocean.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Value("${upload.avatar.dir:/data/ocean/uploads/avatars/}")
    private String avatarDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/uploads/**"
                );

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/user/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/current"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:" + avatarDir);
    }
}
```

关键点：RoleInterceptor 排在 JwtInterceptor 之后（先认证再授权），排除 `/api/user/login`、`/api/user/register`、`/api/user/current`（非管理员接口）。

---

### Task 12: HealthSmsTask 适配

**Files:**
- Modify: `ocean-server/src/main/java/com/ocean/task/HealthSmsTask.java`

- [ ] **Step 1: 注入 UserSettingService 并过滤用户**

```java
package com.ocean.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.SysUser;
import com.ocean.mapper.SysUserMapper;
import com.ocean.service.HealthService;
import com.ocean.service.UserSettingService;
import com.ocean.sms.SmsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class HealthSmsTask {

    @Autowired private HealthService healthService;
    @Autowired private SmsService smsService;
    @Autowired private SysUserMapper sysUserMapper;
    @Autowired private UserSettingService userSettingService;

    @Scheduled(cron = "0 0 8 * * ?")
    public void sendDailySms() {
        log.info(">>>>>> 健康短信任务开始");

        try {
            String content = healthService.buildDailySummary();
            if (content == null) {
                log.info("无健康数据，跳过短信发送");
                return;
            }

            var admins = sysUserMapper.selectList(
                    new LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getRole, "ADMIN")
                            .eq(SysUser::getStatus, 1));

            if (admins.isEmpty()) {
                log.warn("无活跃管理员，跳过推送");
                return;
            }

            int sent = 0;
            for (SysUser admin : admins) {
                try {
                    if (!isSmsEnabled(admin.getId())) {
                        log.info("用户 {} 已关闭短信通知，跳过", admin.getUsername());
                        continue;
                    }
                    boolean ok = smsService.send(admin.getPhone(), content);
                    if (ok) {
                        log.info("健康日报已推送至 {} (微信)", admin.getUsername());
                        sent++;
                    } else {
                        log.error("健康日报推送失败 {}", admin.getUsername());
                    }
                } catch (Exception e) {
                    log.error("健康日报推送异常 {}", admin.getUsername(), e);
                }
            }

            log.info("<<<<<< 健康短信任务完成，推送 {} 条", sent);
        } catch (Exception e) {
            log.error("<<<<<< 健康短信任务失败", e);
        }
    }

    private boolean isSmsEnabled(Long userId) {
        Map<String, String> settings = userSettingService.getUserSettings(userId);
        return !"false".equals(settings.get("sms_enabled"));
    }
}
```

---

### Task 13: 前端 API 层

**Files:**
- Create: `ocean-web/src/api/profile.js`

- [ ] **Step 1: 创建 profile.js**

```js
import request from '../utils/request'

export function getProfile() {
  return request({ url: '/profile', method: 'get' })
}

export function updateProfile(data) {
  return request({ url: '/profile', method: 'put', data })
}

export function changePassword(data) {
  return request({ url: '/profile/password', method: 'put', data })
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({
    url: '/profile/avatar',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getSettings() {
  return request({ url: '/profile/settings', method: 'get' })
}

export function updateSettings(data) {
  return request({ url: '/profile/settings', method: 'put', data })
}

export function getCredentials() {
  return request({ url: '/profile/credentials', method: 'get' })
}

export function saveCredential(data) {
  return request({ url: '/profile/credentials', method: 'post', data })
}

export function deleteCredential(id) {
  return request({ url: `/profile/credentials/${id}`, method: 'delete' })
}
```

---

### Task 14: 前端 Store 更新

**Files:**
- Modify: `ocean-web/src/store/user.js`

- [ ] **Step 1: user.js 增加 avatarUrl**

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

  function setToken(val) {
    token.value = val
    localStorage.setItem('token', val)
  }

  function setUserInfo(val) {
    userInfo.value = val
    localStorage.setItem('userInfo', JSON.stringify(val))
  }

  function setAvatar(url) {
    if (userInfo.value) {
      userInfo.value.avatarUrl = url
      localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  const isAdmin = () => userInfo.value?.role === 'ADMIN'

  return { token, userInfo, setToken, setUserInfo, setAvatar, logout, isAdmin }
})
```

---

### Task 15: 前端 ProfileView.vue

**Files:**
- Create: `ocean-web/src/views/profile/ProfileView.vue`

- [ ] **Step 1: 创建 ProfileView.vue**

```vue
<template>
  <div class="profile-page">
    <h1 class="editorial-page-title">个人中心</h1>
    <p class="editorial-page-subtitle">管理您的账户信息和偏好设置</p>

    <!-- 个人信息 -->
    <div class="editorial-section">
      <h2 class="editorial-section-heading">个人信息</h2>

      <div class="profile-avatar-section">
        <div class="profile-avatar" @click="triggerUpload">
          <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
          <span v-else class="profile-avatar__placeholder">{{ avatarLetter }}</span>
        </div>
        <input
          ref="fileInput"
          type="file"
          accept="image/jpeg,image/png,image/gif"
          style="display:none"
          @change="handleFileChange"
        />
        <span class="editorial-link" @click="triggerUpload">更换头像</span>
      </div>

      <div class="profile-form">
        <div class="profile-form__item">
          <label class="editorial-form-label">用户名</label>
          <input v-model="form.username" class="editorial-input" />
        </div>
        <div class="profile-form__item">
          <label class="editorial-form-label">真实姓名</label>
          <input v-model="form.realName" class="editorial-input" />
        </div>
        <div class="profile-form__item">
          <label class="editorial-form-label">手机号</label>
          <input v-model="form.phone" class="editorial-input" />
        </div>
        <button class="editorial-btn" :disabled="saving" @click="handleSave">
          {{ saving ? '保存中...' : '保存修改' }}
        </button>
      </div>
    </div>

    <!-- 修改密码 -->
    <div class="editorial-section">
      <h2 class="editorial-section-heading">修改密码</h2>
      <div class="profile-form">
        <div class="profile-form__item">
          <label class="editorial-form-label">旧密码</label>
          <input v-model="pwdForm.oldPassword" type="password" class="editorial-input" />
        </div>
        <div class="profile-form__item">
          <label class="editorial-form-label">新密码</label>
          <input v-model="pwdForm.newPassword" type="password" class="editorial-input" />
        </div>
        <div class="profile-form__item">
          <label class="editorial-form-label">确认密码</label>
          <input v-model="pwdForm.confirmPassword" type="password" class="editorial-input" />
        </div>
        <button class="editorial-btn" :disabled="changingPwd" @click="handleChangePassword">
          {{ changingPwd ? '修改中...' : '修改密码' }}
        </button>
      </div>
    </div>

    <!-- 通知设置 -->
    <div class="editorial-section">
      <h2 class="editorial-section-heading">通知设置</h2>
      <div class="profile-settings">
        <div class="profile-settings__item">
          <span class="profile-settings__label">短信通知</span>
          <el-switch v-model="settingsForm.sms_enabled" />
        </div>
        <div class="profile-settings__item">
          <span class="profile-settings__label">ServerChan 推送</span>
          <el-switch v-model="settingsForm.push_enabled" />
          <button class="editorial-btn-outline" @click="showCredentialDialog = true">
            配置Key
          </button>
        </div>
      </div>
    </div>

    <!-- Key配置弹窗 -->
    <el-dialog
      v-model="showCredentialDialog"
      title="ServerChan Key 配置"
      width="420px"
      :close-on-click-modal="false"
    >
      <div v-if="credential">
        <label class="editorial-form-label">当前Key</label>
        <p style="font-family:var(--font-mono);font-size:13px;color:var(--color-text-muted)">
          {{ credential.credentialValue }}
        </p>
      </div>
      <div style="margin-top:16px">
        <label class="editorial-form-label">新Key</label>
        <input v-model="credentialForm.credentialValue" class="editorial-input" placeholder="输入 ServerChan SendKey" />
      </div>
      <template #footer>
        <button class="editorial-btn-outline" @click="showCredentialDialog = false">取消</button>
        <button class="editorial-btn" style="margin-left:8px;padding-left:16px;padding-right:16px" @click="handleSaveCredential">保存</button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../store/user'
import {
  getProfile, updateProfile, changePassword, uploadAvatar,
  getSettings, updateSettings, getCredentials, saveCredential
} from '../../api/profile'

const userStore = useUserStore()

const fileInput = ref(null)
const saving = ref(false)
const changingPwd = ref(false)
const showCredentialDialog = ref(false)

const form = ref({ username: '', realName: '', phone: '' })
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const settingsForm = ref({ sms_enabled: true, push_enabled: true })
const credential = ref(null)
const credentialForm = ref({ credentialKey: 'serverchan_key', credentialValue: '' })

const avatarUrl = computed(() => userStore.userInfo?.avatarUrl || '')
const avatarLetter = computed(() => {
  const name = userStore.userInfo?.realName || userStore.userInfo?.username || '?'
  return name.charAt(0).toUpperCase()
})

onMounted(async () => {
  try {
    const res = await getProfile()
    const u = res.data
    form.value = { username: u.username, realName: u.realName || '', phone: u.phone || '' }
    userStore.setUserInfo({ ...userStore.userInfo, ...u })
  } catch {}

  try {
    const res = await getSettings()
    settingsForm.value = { sms_enabled: res.data.sms_enabled === 'true', push_enabled: res.data.push_enabled === 'true' }
  } catch {}

  try {
    const res = await getCredentials()
    const list = res.data || []
    credential.value = list.find(c => c.credentialKey === 'serverchan_key') || null
  } catch {}
})

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) { ElMessage.error('文件大小不能超过2MB'); return }
  try {
    const res = await uploadAvatar(file)
    userStore.setAvatar(res.data.avatarUrl)
    ElMessage.success('头像已更新')
  } catch {}
}

async function handleSave() {
  saving.value = true
  try {
    await updateProfile(form.value)
    userStore.setUserInfo({ ...userStore.userInfo, username: form.value.username, realName: form.value.realName, phone: form.value.phone })
    ElMessage.success('个人信息已更新')
  } catch {} finally { saving.value = false }
}

async function handleChangePassword() {
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次密码输入不一致')
    return
  }
  if (pwdForm.value.newPassword.length < 6) {
    ElMessage.error('新密码至少6位')
    return
  }
  changingPwd.value = true
  try {
    await changePassword(pwdForm.value)
    pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    ElMessage.success('密码已修改')
  } catch {} finally { changingPwd.value = false }
}

watch([() => settingsForm.value.sms_enabled, () => settingsForm.value.push_enabled], () => {
  updateSettings({
    settings: {
      sms_enabled: String(settingsForm.value.sms_enabled),
      push_enabled: String(settingsForm.value.push_enabled)
    }
  })
})

async function handleSaveCredential() {
  if (!credentialForm.value.credentialValue) { ElMessage.error('请输入Key'); return }
  try {
    await saveCredential(credentialForm.value)
    ElMessage.success('Key已保存')
    showCredentialDialog.value = false
    const res = await getCredentials()
    const list = res.data || []
    credential.value = list.find(c => c.credentialKey === 'serverchan_key') || null
  } catch {}
}
</script>

<style scoped>
.profile-page { max-width: 600px; }

.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.profile-avatar {
  width: 64px; height: 64px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--color-divider-strong);
}
.profile-avatar img {
  width: 100%; height: 100%;
  object-fit: cover;
}
.profile-avatar__placeholder {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 100%;
  font-family: var(--font-serif); font-size: 24px;
  color: var(--color-text-muted); background: var(--color-surface);
}

.profile-form {}
.profile-form__item { margin-bottom: 16px; }
.profile-form .editorial-btn { width: 100%; margin-top: 8px; }

.profile-settings { display: flex; flex-direction: column; gap: 16px; }
.profile-settings__item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.profile-settings__label {
  font-size: 13px;
  color: var(--color-text);
}
</style>
```

---

### Task 16: 前端 MainLayout — 头像下拉菜单 + footer 版本号

**Files:**
- Modify: `ocean-web/src/layout/MainLayout.vue`

- [ ] **Step 1: 修改 MainLayout.vue**

模板部分替换导航栏右侧区域（从 `<span v-if="isAdmin" class="editorial-tag"...>` 到 `</a>` 这三行）：

```vue
<!-- 替换导航栏右侧 -->
<span v-if="isAdmin" class="editorial-tag" style="margin-right: 12px;">ADMIN</span>

<div class="nav-user-menu" @mouseenter="showUserMenu = true" @mouseleave="showUserMenu = false">
  <div class="nav-user-avatar">
    <img v-if="userInfo?.avatarUrl" :src="userInfo.avatarUrl" alt="" />
    <span v-else class="nav-user-avatar__placeholder">{{ avatarLetter }}</span>
  </div>
  <div v-show="showUserMenu" class="nav-user-dropdown"
       @mouseenter="showUserMenu = true" @mouseleave="showUserMenu = false">
    <router-link to="/app/profile" class="nav-user-dropdown__item">个人中心</router-link>
    <a class="nav-user-dropdown__item" @click="handleLogout">退出登录</a>
  </div>
</div>
```

在 `<main class="editorial-content">` 之后、`</div>` 之前增加 footer：

```vue
<footer class="editorial-footer">
  <span>海洋环境预报系统 v{{ __APP_VERSION__ }}</span>
</footer>
```

script 部分增加：

```js
const showUserMenu = ref(false)

const avatarLetter = computed(() => {
  const name = userInfo.value?.realName || userInfo.value?.username || '?'
  return name.charAt(0).toUpperCase()
})
```

CSS 增加：

```css
.nav-user-menu {
  position: relative;
  margin-left: 16px;
}
.nav-user-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid var(--color-divider-strong);
}
.nav-user-avatar img {
  width: 100%; height: 100%;
  object-fit: cover;
}
.nav-user-avatar__placeholder {
  display: flex; align-items: center; justify-content: center;
  width: 100%; height: 100%;
  font-family: var(--font-serif); font-size: 14px;
  color: var(--color-text-muted); background: var(--color-surface);
}
.nav-user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  background: var(--color-bg);
  border: 1px solid var(--color-divider-strong);
  min-width: 120px;
  z-index: 200;
  padding: 8px 0;
}
.nav-user-dropdown__item {
  display: block;
  padding: 10px 20px;
  font-size: 13px;
  color: var(--color-text-muted);
  text-decoration: none;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}
.nav-user-dropdown__item:hover {
  background: var(--color-surface);
  color: var(--color-text);
}
.editorial-footer {
  padding: 16px 40px;
  border-top: 1px solid var(--color-divider);
  text-align: right;
}
.editorial-footer span {
  font-family: var(--font-mono);
  font-size: 11px;
  color: var(--color-text-muted);
}
```

---

### Task 17: 前端路由 + 版本号注入

**Files:**
- Modify: `ocean-web/src/router/index.js`
- Modify: `ocean-web/vite.config.js`

- [ ] **Step 1: 路由增加 profile**

在 `children` 数组末尾（`ocean-health` 路由之后）增加：

```js
{
  path: 'profile',
  name: 'Profile',
  component: () => import('../views/profile/ProfileView.vue'),
  meta: { title: '个人中心' }
}
```

- [ ] **Step 2: vite.config.js 注入版本号**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { readFileSync } from 'fs'

const pkg = JSON.parse(readFileSync('./package.json', 'utf-8'))

export default defineConfig({
  plugins: [vue()],
  define: {
    __APP_VERSION__: JSON.stringify(pkg.version)
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

`__APP_VERSION__` 是 Vite 编译时常量替换，构建时直接替换为字符串字面量（如 `"1.0.0"`），模板和 script 中均可直接使用。

- [ ] **Step 3: 验证版本号**

```bash
cd ocean-web && npm run dev
```

打开浏览器，确认页面底部 footer 显示 `v1.0.0`。

---

### Task 18: 端到端验证

- [ ] **Step 1: 启动后端**

```bash
cd ocean-server && mvn spring-boot:run
```

- [ ] **Step 2: 启动前端**

```bash
cd ocean-web && npm run dev
```

- [ ] **Step 3: 验证清单**

| 验证项 | 预期 |
|---|---|
| 个人中心页面 `/app/profile` | 正常加载，显示用户信息 |
| 编辑个人信息 | 用户名/真实姓名/手机号可修改保存 |
| 头像上传 | 点击头像可上传图片，缩略图更新 |
| 修改密码 | 旧密码错误会提示，新密码不一致会提示，正确修改成功 |
| 短信开关 | 切换开关后刷新页面，状态保持 |
| ServerChan Key 配置 | 弹窗输入 Key，保存后脱敏展示 |
| 导航栏头像 | 登录后显示头像/首字母，下拉菜单含个人中心和退出 |
| 底部版本号 | 显示 `v1.0.0` |
| 普通用户无法调管理员接口 | 用 USER 角色 token 调 `/api/user/page` 返回 403 |
| 管理接口拒绝 `current` | `/api/user/current` 不走 RoleInterceptor，普通用户可调 |

- [ ] **Step 4: 提交所有变更**

```bash
git add -A
git commit -m "feat: complete user system with profile, settings, credentials, role interceptor"
```

---

## 执行顺序

```
Task 1  (数据库迁移)     ─┐
Task 2  (实体)           ├─ 数据库层，必须先做
Task 3  (Mapper)        ─┘
Task 4  (AES工具)        ─┐
Task 5  (DTO)            ├─ 工具/DTO，无依赖
Task 6  (UserSettingService)  ─┐
Task 7  (UserCredentialService) ├─ 依赖实体和Mapper
Task 8  (ProfileController)    ─┤ 依赖Service
Task 9  (SysUserService补充)   ─┘
Task 10 (已有实体/VO修改)  — 可并行
Task 11 (RoleInterceptor)  — 依赖Task 10
Task 12 (HealthSmsTask)    — 依赖Task 6
Task 13 (前端API)           ─┐
Task 14 (前端Store)         ├─ 前端层，可并行
Task 15 (ProfileView)       ├─ 依赖Task 13/14
Task 16 (MainLayout)        ├─ 依赖Task 14
Task 17 (路由+Vite)         ─┘ 依赖Task 15
Task 18 (端到端验证)       — 最后
```

---

## Code Review 已修复（2026-05-25）

### 关键

- [x] **1. `SysUserServiceImpl.login()` 未设置 `LoginVO.avatarUrl`** — 添加 `vo.setAvatarUrl(user.getAvatarUrl())`
- [x] **2. `ProfileController.updateProfile()` 构造不完整的 `UserSaveDTO`** — 新增 `SysUserService.updateProfile(Long, ProfileUpdateDTO)` 专用方法，只更新 username/realName/phone

### 重要

- [x] **3. 新代码抛 `RuntimeException` 而非 `BusinessException`** — `SysUserServiceImpl` (3处)、`UserCredentialServiceImpl` (1处) 全部改为 `BusinessException`
- [x] **4. `UserSettingServiceImpl.updateSettings()` 存在读写竞争** — 改为 `LambdaUpdateWrapper` 原子更新 + `DuplicateKeyException` 兜底重试
- [x] **5. `AesUtil.padKey()` 密钥派生太弱** — 用 SHA-256 哈希后取前 16 字节
- [x] **6. `PasswordChangeDTO` 缺少 `@Size(min=6)` 密码长度校验（后端）**
- [x] **7. `ProfileView.vue` 中 9 个空 `catch {}` 块** — 全部添加 `console.error()` 日志
- [x] **8. 设置 `watch` 在页面加载时触发无效 API 调用** — 添加 `loaded` 标志位，`onMounted` 完成后才启用 watch

### 轻微

- [x] **9. `SecureRandom` 每次加密都 new 实例** — 改为 `static final` 单例
- [x] **10. 手机号缺少格式校验** — 前端 `handleSave()` 添加 `/^1[3-9]\d{9}$/` 格式校验
- [x] **11. 旧头像文件未清理** — `uploadAvatar()` 先删除旧头像文件再保存新文件
- [x] **12. `SysUser.avatarUrl` 缺少 `@TableField("avatar_url")` 显式映射**
- [x] **13. `AesUtil.mask()` 对短字符串可能泄露前缀** — 6字符以下全掩码，8字符以下只显示首尾各2字符
