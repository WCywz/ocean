# 用户体系完善 — 设计

## 概述

完善现有用户系统，核心补全：个人中心（信息编辑 + 头像上传 + 修改密码）、通知偏好设置（SMS/ServerChan）、密钥管理（AES 加密存储）、前端版本号展示、后端角色权限修复。

## 数据库

### sys_user 变更

```sql
ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT '头像相对URL路径，如 /uploads/avatars/1_1700000000.jpg';
```

`avatar_url` 存储的是 URL 路径（非文件系统路径），前端通过此 URL 加载头像静态资源。SysUser 实体、UserVO、LoginVO 同步新增 `avatarUrl` 字段。

### 新建 user_setting — 偏好开关

```sql
CREATE TABLE user_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    setting_key VARCHAR(50) NOT NULL COMMENT '设置键',
    setting_value VARCHAR(500) NOT NULL DEFAULT '' COMMENT '设置值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    CONSTRAINT fk_user_setting_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) COMMENT '用户设置表';
```

默认设置项（代码中定义默认值，不预插入）：
- `sms_enabled`: `true` — 是否接收短信通知
- `push_enabled`: `true` — 是否接收 ServerChan 推送

### 新建 user_credential — 通知密钥

```sql
CREATE TABLE user_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    credential_key VARCHAR(50) NOT NULL COMMENT '密钥类型',
    credential_value VARCHAR(1000) NOT NULL COMMENT '密钥值(AES加密)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_credential (user_id, credential_key),
    CONSTRAINT fk_user_credential_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) COMMENT '用户密钥表(AES加密存储)';
```

密钥类型：
- `serverchan_key`: ServerChan SendKey

## 后端

### 新增 Entity/Service/Mapper

| 类 | 说明 |
|---|---|
| `UserSetting` | user_setting 实体 |
| `UserSettingMapper` | MyBatis-Plus BaseMapper |
| `UserCredential` | user_credential 实体 |
| `UserCredentialMapper` | MyBatis-Plus BaseMapper |
| `UserSettingService` | 设置 CRUD + 默认值初始化 |
| `UserCredentialService` | 密钥 CRUD + AES 加解密 |

### 新增 DTO

| 类 | 字段 | 说明 |
|---|---|---|
| `ProfileUpdateDTO` | username, realName, phone | 个人资料编辑 |
| `PasswordChangeDTO` | oldPassword, newPassword, confirmPassword | 修改密码 |
| `SettingsUpdateDTO` | Map<String, String> | 设置批量更新 |
| `CredentialSaveDTO` | credentialKey, credentialValue | 密钥添加/更新 |

### 已有类修改

| 类 | 变更 |
|---|---|
| `SysUser` 实体 | 新增 `avatarUrl` 字段（`@TableField("avatar_url")`） |
| `UserVO` | 新增 `avatarUrl` |
| `LoginVO` | 新增 `avatarUrl` |
| `WebMvcConfig` | 注册 RoleInterceptor，新增头像静态资源映射 |

### 新增 ProfileController — `/api/profile`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/profile` | 获取当前用户信息 |
| PUT | `/api/profile` | 编辑用户名、真实姓名、手机号 |
| PUT | `/api/profile/password` | 修改密码（需旧密码验证） |
| POST | `/api/profile/avatar` | 上传头像（multipart） |

### 设置接口 — `/api/profile/settings`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/profile/settings` | 获取所有设置项（若无记录返回默认值） |
| PUT | `/api/profile/settings` | 批量更新设置 `{sms_enabled: true, ...}`（合并更新，只更新传入的 key，未传入的保持不变） |

### 密钥接口 — `/api/profile/credentials`

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/profile/credentials` | 获取密钥列表（value 脱敏：`SCT1****ef`） |
| POST | `/api/profile/credentials` | 添加/更新密钥 |
| DELETE | `/api/profile/credentials/{id}` | 删除密钥 |

### AES 加密

密钥存在 `application.yml` 的 `credential.encrypt.secret` 配置项，AES/CBC/PKCS5Padding 加解密。调用 ServerChan API 时解密后使用，返回前端时脱敏展示。

### 角色拦截器

新建 `RoleInterceptor`，在 WebMvcConfig 中注册，仅拦截 `/api/user/**` 路径，从请求属性中读取 role（由 JwtInterceptor 设置），非 ADMIN 返回 403。

### 已有 UserController 修复

`/api/user/**` 路径全部走 RoleInterceptor，不再依赖前端路由守卫做权限控制。

### 头像上传

- 上传目录：`${upload.avatar.dir:/data/ocean/uploads/avatars/}`
- 文件名：`{userId}_{timestamp}.{ext}`
- 静态资源映射：`WebMvcConfig` 中映射 `/uploads/avatars/**` 到上传目录
- 大小限制：2MB
- 格式限制：jpg、png、gif

## 前端

### 导航栏

顶部导航栏右侧新增用户头像（圆形缩略图） + 下拉菜单：
- 个人中心 → `/app/profile`
- 退出登录

默认头像使用用户名首字母圆形色块。

### ProfileView.vue — 个人中心

单页面，三个区块：

**个人信息**
- 头像：可点击上传（隐藏 file input），圆形展示缩略图
- 表单：用户名、真实姓名、手机号
- 保存按钮

**修改密码**
- 旧密码、新密码、确认密码
- 修改按钮

**通知设置**
- 短信通知：Switch 开关
- ServerChan 通知：Switch 开关 + Key 配置按钮（弹出输入框，展示时脱敏）

### 风格

沿用 editorial 设计系统：
- 标题：Georgia 衬线字体
- 色彩：黑/白/灰单色系
- `#c0392b` 仅用于错误提示
- 无圆角、无阴影
- 区块之间用分割线区分

### 版本号

页面底部 footer：`v{version}`，构建时通过 Vite `import.meta.env` 从 `package.json` 注入。

### 路由

```js
{ path: '/app/profile', component: ProfileView, meta: { title: '个人中心' } }
```

### Store 更新

`user.js` store 新增：
- `avatarUrl` 字段
- 登录时从后端获取
- 上传成功后更新

## 现有功能集成

### HealthSmsTask 适配

定时短信任务发送前检查用户的 `sms_enabled` 设置项：遍历管理员时查询 `user_setting` 表，`sms_enabled = false` 的用户跳过不发送。默认无记录视为 `true`（接收短信）。

## 兼容性

- 新增表不影响现有功能
- sys_user 只加一个 nullable 字段 `avatar_url`，不影响已有逻辑
- 已有 API 路径和行为不变
- 用户无设置记录时返回默认值（sms_enabled=true, push_enabled=true）
- 无密钥记录时返回空列表
