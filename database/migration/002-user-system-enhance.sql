-- ============================================================
-- Migration: 002-user-system-enhance
-- Description: Add avatar column, user settings table, and user credentials table
-- Created: 2026-05-25
-- ============================================================

-- 用户头像列（使用条件判断实现幂等，MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS）
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'sys_user'
    AND COLUMN_NAME = 'avatar_url');

SET @sql = IF(@col_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN avatar_url VARCHAR(500) DEFAULT NULL COMMENT ''头像URL路径''',
    'SELECT ''Column avatar_url already exists, skipping.'' AS msg');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 用户偏好设置表
CREATE TABLE IF NOT EXISTS user_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    setting_key VARCHAR(50) NOT NULL COMMENT '设置键',
    setting_value VARCHAR(500) NOT NULL DEFAULT '' COMMENT '设置值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_setting (user_id, setting_key),
    CONSTRAINT fk_user_setting_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户偏好设置表';

-- 用户密钥表（AES加密存储）
CREATE TABLE IF NOT EXISTS user_credential (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    credential_key VARCHAR(50) NOT NULL COMMENT '密钥类型',
    credential_value VARCHAR(2000) NOT NULL COMMENT '密钥值(AES加密)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_credential (user_id, credential_key),
    CONSTRAINT fk_user_credential_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT '用户密钥表';

