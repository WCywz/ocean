-- ============================================
-- 海洋环境预报系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- ============================================

CREATE DATABASE IF NOT EXISTS ocean_forecast
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ocean_forecast;

-- ============================================
-- 1. 系统用户表
-- ============================================
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    username    VARCHAR(50)     NOT NULL                 COMMENT '用户名',
    password    VARCHAR(255)    NOT NULL                 COMMENT '密码（BCrypt加密）',
    real_name   VARCHAR(50)     DEFAULT ''               COMMENT '真实姓名',
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER'  COMMENT '角色：ADMIN-管理员, USER-普通用户',
    status      TINYINT         NOT NULL DEFAULT 1       COMMENT '状态：1-启用, 0-禁用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 默认管理员账号: admin / admin123
-- 默认普通用户账号: user / user123
INSERT INTO sys_user (username, password, real_name, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '系统管理员', 'ADMIN', 1),
('user',  '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5Eh', '普通用户',   'USER',  1);

-- ============================================
-- 2. 预报模型配置表
-- ============================================
DROP TABLE IF EXISTS forecast_model;
CREATE TABLE forecast_model (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    model_name      VARCHAR(100)    NOT NULL                 COMMENT '模型名称',
    model_type      VARCHAR(50)     NOT NULL                 COMMENT '模型类型：SST-海表温度, CHL-叶绿素浓度',
    params_config   TEXT                                     COMMENT '模型参数配置（JSON格式）',
    cron_expression VARCHAR(50)     DEFAULT ''               COMMENT '运行周期Cron表达式',
    status          VARCHAR(20)     NOT NULL DEFAULT 'STOPPED' COMMENT '状态：RUNNING-运行中, STOPPED-已停止, ERROR-异常',
    last_run_time   DATETIME        DEFAULT NULL             COMMENT '最近运行时间',
    description     VARCHAR(500)    DEFAULT ''               COMMENT '模型描述',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预报模型配置表';

-- 初始化示例模型数据
INSERT INTO forecast_model (model_name, model_type, params_config, cron_expression, status, description) VALUES
('海表温度预报模型 v1', 'SST', '{"algorithm":"ROMS","resolution":"0.1deg","forecast_days":7,"depth_levels":10}', '0 0 6 * * ?', 'RUNNING', '基于ROMS模型的区域海表温度预报'),
('叶绿素浓度预报模型 v1', 'CHL', '{"algorithm":"BIOCHEM","resolution":"0.05deg","forecast_days":5,"satellite_source":"MODIS"}', '0 0 8 * * ?', 'RUNNING', '基于生物化学模型的叶绿素浓度预报'),
('海表温度预报模型 v2', 'SST', '{"algorithm":"HYCOM","resolution":"0.08deg","forecast_days":10,"assimilation":"3DVAR"}', '0 0 6,18 * * ?', 'STOPPED', '高分辨率海表温度预报（测试中）');

-- ============================================
-- 3. 预报数据记录表
-- ============================================
DROP TABLE IF EXISTS forecast_record;
CREATE TABLE forecast_record (
    id              BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    model_id        BIGINT          NOT NULL                 COMMENT '关联模型ID',
    data_type       VARCHAR(20)     NOT NULL                 COMMENT '数据类型：SST-海表温度, CHL-叶绿素浓度',
    forecast_date   DATE            NOT NULL                 COMMENT '预报日期',
    location_name   VARCHAR(100)    NOT NULL                 COMMENT '观测点名称',
    longitude       DECIMAL(10,6)   NOT NULL                 COMMENT '经度',
    latitude        DECIMAL(10,6)   NOT NULL                 COMMENT '纬度',
    value           DOUBLE          NOT NULL                 COMMENT '预报数值',
    unit            VARCHAR(20)     NOT NULL DEFAULT ''      COMMENT '单位',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_model_id (model_id),
    KEY idx_data_type (data_type),
    KEY idx_forecast_date (forecast_date),
    KEY idx_location (location_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预报数据记录表';

-- 初始化示例预报数据（海表温度 - 摄氏度）
INSERT INTO forecast_record (model_id, data_type, forecast_date, location_name, longitude, latitude, value, unit) VALUES
(1, 'SST', '2026-04-28', '渤海观测站A', 119.50, 38.50, 15.8, '°C'),
(1, 'SST', '2026-04-28', '黄海观测站B', 122.50, 36.00, 17.2, '°C'),
(1, 'SST', '2026-04-28', '东海观测站C', 124.00, 30.00, 22.5, '°C'),
(1, 'SST', '2026-04-28', '南海观测站D', 116.00, 18.00, 27.3, '°C'),
(1, 'SST', '2026-04-29', '渤海观测站A', 119.50, 38.50, 16.1, '°C'),
(1, 'SST', '2026-04-29', '黄海观测站B', 122.50, 36.00, 17.5, '°C'),
(1, 'SST', '2026-04-29', '东海观测站C', 124.00, 30.00, 22.8, '°C'),
(1, 'SST', '2026-04-29', '南海观测站D', 116.00, 18.00, 27.6, '°C'),
(1, 'SST', '2026-04-30', '渤海观测站A', 119.50, 38.50, 16.5, '°C'),
(1, 'SST', '2026-04-30', '黄海观测站B', 122.50, 36.00, 17.9, '°C'),
(1, 'SST', '2026-04-30', '东海观测站C', 124.00, 30.00, 23.2, '°C'),
(1, 'SST', '2026-04-30', '南海观测站D', 116.00, 18.00, 28.0, '°C');

-- 初始化示例预报数据（叶绿素浓度 - mg/m³）
INSERT INTO forecast_record (model_id, data_type, forecast_date, location_name, longitude, latitude, value, unit) VALUES
(2, 'CHL', '2026-04-28', '渤海观测站A', 119.50, 38.50, 2.35, 'mg/m³'),
(2, 'CHL', '2026-04-28', '黄海观测站B', 122.50, 36.00, 1.80, 'mg/m³'),
(2, 'CHL', '2026-04-28', '东海观测站C', 124.00, 30.00, 1.25, 'mg/m³'),
(2, 'CHL', '2026-04-28', '南海观测站D', 116.00, 18.00, 0.50, 'mg/m³'),
(2, 'CHL', '2026-04-29', '渤海观测站A', 119.50, 38.50, 2.42, 'mg/m³'),
(2, 'CHL', '2026-04-29', '黄海观测站B', 122.50, 36.00, 1.85, 'mg/m³'),
(2, 'CHL', '2026-04-29', '东海观测站C', 124.00, 30.00, 1.30, 'mg/m³'),
(2, 'CHL', '2026-04-29', '南海观测站D', 116.00, 18.00, 0.55, 'mg/m³'),
(2, 'CHL', '2026-04-30', '渤海观测站A', 119.50, 38.50, 2.50, 'mg/m³'),
(2, 'CHL', '2026-04-30', '黄海观测站B', 122.50, 36.00, 1.92, 'mg/m³'),
(2, 'CHL', '2026-04-30', '东海观测站C', 124.00, 30.00, 1.35, 'mg/m³'),
(2, 'CHL', '2026-04-30', '南海观测站D', 116.00, 18.00, 0.60, 'mg/m³');

-- ============================================
-- 4. 海洋观测数据表（叶绿素浓度 + 深度）
-- ============================================
DROP TABLE IF EXISTS ocean_data;
CREATE TABLE ocean_data (
    id          BIGINT          NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    lat         DECIMAL(10,6)   NOT NULL                 COMMENT '纬度',
    lon         DECIMAL(10,6)   NOT NULL                 COMMENT '经度',
    time        DATE            NOT NULL                 COMMENT '观测日期',
    chl         DOUBLE          NOT NULL                 COMMENT '叶绿素浓度',
    depth       DOUBLE          NOT NULL                 COMMENT '深度（米）',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_lat_lon_time (lat, lon, time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海洋观测数据表';

-- 示例数据（渤海/黄海/东海 三个点位，深度 0m/10m/30m，3天）
INSERT INTO ocean_data (lat, lon, time, chl, depth) VALUES
-- 4月28日
(38.500000, 119.500000, '2026-04-28', 2.35, 0),
(38.500000, 119.500000, '2026-04-28', 2.10, 10),
(38.500000, 119.500000, '2026-04-28', 1.45, 30),
(36.000000, 122.500000, '2026-04-28', 1.80, 0),
(36.000000, 122.500000, '2026-04-28', 1.55, 10),
(36.000000, 122.500000, '2026-04-28', 0.95, 30),
(30.000000, 124.000000, '2026-04-28', 1.25, 0),
(30.000000, 124.000000, '2026-04-28', 1.05, 10),
(30.000000, 124.000000, '2026-04-28', 0.65, 30),
-- 4月29日
(38.500000, 119.500000, '2026-04-29', 2.42, 0),
(38.500000, 119.500000, '2026-04-29', 2.18, 10),
(38.500000, 119.500000, '2026-04-29', 1.52, 30),
(36.000000, 122.500000, '2026-04-29', 1.85, 0),
(36.000000, 122.500000, '2026-04-29', 1.60, 10),
(36.000000, 122.500000, '2026-04-29', 1.02, 30),
(30.000000, 124.000000, '2026-04-29', 1.30, 0),
(30.000000, 124.000000, '2026-04-29', 1.10, 10),
(30.000000, 124.000000, '2026-04-29', 0.70, 30),
-- 4月30日
(38.500000, 119.500000, '2026-04-30', 2.50, 0),
(38.500000, 119.500000, '2026-04-30', 2.25, 10),
(38.500000, 119.500000, '2026-04-30', 1.58, 30),
(36.000000, 122.500000, '2026-04-30', 1.92, 0),
(36.000000, 122.500000, '2026-04-30', 1.68, 10),
(36.000000, 122.500000, '2026-04-30', 1.08, 30),
(30.000000, 124.000000, '2026-04-30', 1.35, 0),
(30.000000, 124.000000, '2026-04-30', 1.15, 10),
(30.000000, 124.000000, '2026-04-30', 0.72, 30);

-- ============================================
-- 5. 预报表复合索引（先过滤后聚合）
-- ============================================

-- 地图网格聚合查询索引
CREATE INDEX idx_filter ON forecast_record (data_type, forecast_date, longitude, latitude);

-- 单点位趋势查询索引
CREATE INDEX idx_point_trend ON forecast_record (data_type, longitude, latitude, forecast_date);
