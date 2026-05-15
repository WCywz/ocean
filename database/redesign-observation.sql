-- ============================================================
-- 数据库重新设计（观测 + 预报部分）
-- ============================================================

-- ============================================================
-- 一、观测数据模块
-- 变量: chl（叶绿素）、so（盐度）、thetao（温度）
-- 保留近 2 年热数据，按月分区；历史数据通过 .nc 文件下载
-- ============================================================

-- -----------------------------------------------------------
-- 1. 观测数据主表
-- -----------------------------------------------------------
CREATE TABLE observation_data (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    variable    VARCHAR(10)  NOT NULL COMMENT '变量: chl/so/thetao',
    obs_time    DATE         NOT NULL COMMENT '观测时间',
    depth       DOUBLE       NOT NULL COMMENT '深度(m)，表层=0',
    lat         DECIMAL(8,6) NOT NULL COMMENT '纬度',
    lon         DECIMAL(9,6) NOT NULL COMMENT '经度',
    value       DOUBLE       NOT NULL COMMENT '观测值',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, obs_time),
    INDEX idx_query (variable, lat, lon, depth, obs_time),
    INDEX idx_grid  (variable, depth, obs_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY RANGE (TO_DAYS(obs_time)) (
    PARTITION p202408 VALUES LESS THAN (TO_DAYS('2024-09-01')),
    PARTITION p202409 VALUES LESS THAN (TO_DAYS('2024-10-01')),
    PARTITION p202410 VALUES LESS THAN (TO_DAYS('2024-11-01')),
    PARTITION p202411 VALUES LESS THAN (TO_DAYS('2024-12-01')),
    PARTITION p202412 VALUES LESS THAN (TO_DAYS('2025-01-01')),
    PARTITION p202501 VALUES LESS THAN (TO_DAYS('2025-02-01')),
    PARTITION p202502 VALUES LESS THAN (TO_DAYS('2025-03-01')),
    PARTITION p202503 VALUES LESS THAN (TO_DAYS('2025-04-01')),
    PARTITION p202504 VALUES LESS THAN (TO_DAYS('2025-05-01')),
    PARTITION p202505 VALUES LESS THAN (TO_DAYS('2025-06-01')),
    PARTITION p202506 VALUES LESS THAN (TO_DAYS('2025-07-01')),
    PARTITION p202507 VALUES LESS THAN (TO_DAYS('2025-08-01')),
    PARTITION p202508 VALUES LESS THAN (TO_DAYS('2025-09-01')),
    PARTITION p202509 VALUES LESS THAN (TO_DAYS('2025-10-01')),
    PARTITION p202510 VALUES LESS THAN (TO_DAYS('2025-11-01')),
    PARTITION p202511 VALUES LESS THAN (TO_DAYS('2025-12-01')),
    PARTITION p202512 VALUES LESS THAN (TO_DAYS('2026-01-01')),
    PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
    PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
    PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
    PARTITION p202604 VALUES LESS THAN (TO_DAYS('2026-05-01')),
    PARTITION p202605 VALUES LESS THAN (TO_DAYS('2026-06-01')),
    PARTITION p202606 VALUES LESS THAN (TO_DAYS('2026-07-01')),
    PARTITION p_future  VALUES LESS THAN MAXVALUE
);

-- -----------------------------------------------------------
-- 2. 格点坐标缓存表（仅供后端 DISTINCT 加速，非站点概念）
-- -----------------------------------------------------------
CREATE TABLE observation_grid_cache (
    id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    lat DECIMAL(8,6) NOT NULL,
    lon DECIMAL(9,6) NOT NULL,
    UNIQUE KEY uk_lat_lon (lat, lon)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 3. 归档文件表（.nc 文件下载索引）
-- -----------------------------------------------------------
CREATE TABLE observation_archive (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    variable    VARCHAR(10)  NOT NULL COMMENT '变量: chl/so/thetao',
    file_name   VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path   VARCHAR(500) NOT NULL COMMENT '文件路径/URL',
    file_size   BIGINT       DEFAULT NULL COMMENT '文件大小(bytes)',
    time_start  DATE         NOT NULL COMMENT '数据起始时间',
    time_end    DATE         NOT NULL COMMENT '数据结束时间',
    description VARCHAR(500) DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 4. 月度维护：淘汰旧分区 + 新增下月分区
-- 定时任务每月 1 号执行，示例：
--
--   ALTER TABLE observation_data DROP PARTITION p202408;
--   ALTER TABLE observation_data REORGANIZE PARTITION p_future INTO (
--       PARTITION p202607 VALUES LESS THAN (TO_DAYS('2026-08-01')),
--       PARTITION p_future   VALUES LESS THAN MAXVALUE
--   );
-- -----------------------------------------------------------


-- ============================================================
-- 二、预报数据模块
-- 变量: sst（海表温度）、chl（叶绿素浓度）
-- 网格分辨率: 0.25° × 0.25°
-- 数据量: ~391 格点 × 2 变量 = 782 行/天，每年 ~28.5 万行
-- ============================================================

-- -----------------------------------------------------------
-- 1. 预报网格主表
-- 每次模型运行产生未来 7 天的格点预报
-- UNIQUE KEY 含 version_id，不同版本的同日预报并存
-- -----------------------------------------------------------
CREATE TABLE forecast_grid (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    model_id      BIGINT       NOT NULL COMMENT '模型ID（冗余，免JOIN查模型信息）',
    version_id    BIGINT       NOT NULL COMMENT '模型版本ID',
    variable      VARCHAR(10)  NOT NULL COMMENT '变量: sst/chl',
    forecast_date DATE         NOT NULL COMMENT '预报目标日期',
    depth         DOUBLE       NOT NULL DEFAULT 0 COMMENT '深度(m)，sst=0',
    lat           DECIMAL(8,6) NOT NULL COMMENT '纬度（格点）',
    lon           DECIMAL(9,6) NOT NULL COMMENT '经度（格点）',
    value         DOUBLE       NOT NULL COMMENT '预报值',
    unit          VARCHAR(20)  NOT NULL DEFAULT '' COMMENT '单位: degree_C/mg_m3',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_grid (variable, forecast_date, depth, lat, lon, version_id),
    INDEX idx_map        (variable, forecast_date, depth),
    INDEX idx_trend      (variable, lat, lon, forecast_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 数据量估算：
--   391 格点 × 2 变量 = 782 行/天
--   1 个活跃版本 × 365 天 × 782 = 28.5 万行/年
--   2 年 = 57 万行，多版本并行也只翻小倍数，不用分区

-- -----------------------------------------------------------
-- 2. 地图热力图查询（最核心场景）
-- SELECT lat, lon, value
-- FROM forecast_grid
-- WHERE variable = 'sst'
--   AND forecast_date = '2026-05-15'
--   AND depth = 0
-- ORDER BY lat, lon;
-- → 命中 idx_map，返回 391 行
-- -----------------------------------------------------------

-- -----------------------------------------------------------
-- 3. 点位趋势查询
-- SELECT forecast_date, value
-- FROM forecast_grid
-- WHERE variable = 'sst'
--   AND lat = 30.5 AND lon = 123.0
--   AND forecast_date BETWEEN '2026-01-01' AND '2026-06-01';
-- → 命中 idx_trend，返回 ~180 行
-- -----------------------------------------------------------

-- -----------------------------------------------------------
-- 4. CHL 概率模式（超阈值概率）
-- 从前端需求看，概率 = 在 date_start ~ date_end 范围内
-- 值超过阈值的比例。直接从 forecast_grid 计算：
--
-- SELECT lat, lon,
--        SUM(CASE WHEN value > 5.0 THEN 1 ELSE 0 END) / COUNT(*) AS probability
-- FROM forecast_grid
-- WHERE variable = 'chl'
--   AND forecast_date BETWEEN '2026-05-01' AND '2026-05-31'
--   AND depth = 0
-- GROUP BY lat, lon;
--
-- 391 个格点 × 31 天 = ~12,000 行参与计算，秒级响应
-- -----------------------------------------------------------


-- ============================================================
-- 三、站点模块
-- 站点按「近岸/近海/远海 × 北部/南部」分类
-- 海域 bbox 为前端静态常量，不单独建表
-- ============================================================

-- -----------------------------------------------------------
-- 1. 监测站点表
-- 分类: distance（近岸/近海/远海） + region（北部/南部）
-- 每个站点属于一个健康评估区域
-- -----------------------------------------------------------
CREATE TABLE monitoring_station (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    station_name   VARCHAR(100) NOT NULL COMMENT '站点名称',
    lat            DECIMAL(8,6) NOT NULL,
    lon            DECIMAL(9,6) NOT NULL,
    distance       VARCHAR(10)  NOT NULL COMMENT '近岸/近海/远海',
    region         VARCHAR(10)  NOT NULL COMMENT '北部/南部',
    health_zone_id BIGINT       DEFAULT NULL COMMENT 'FK → health_zone.id',
    is_active      TINYINT      NOT NULL DEFAULT 1,
    sort_order     INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_name (station_name),
    INDEX idx_distance_region (distance, region),
    INDEX idx_health_zone (health_zone_id),
    INDEX idx_lat_lon (lat, lon)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 2. 站点查询：按分类或健康区域筛选
-- 按 distance + region：
-- SELECT * FROM monitoring_station
-- WHERE distance = '近岸' AND region = '北部' AND is_active = 1
-- ORDER BY sort_order;
--
-- 按健康区域查站点（某区域评级 bad 时查影响站点）：
-- SELECT ms.* FROM monitoring_station ms
-- JOIN health_record hr ON hr.zone_id = ms.health_zone_id
-- WHERE hr.assess_date = CURDATE() AND hr.overall_grade = 'bad';
-- -----------------------------------------------------------

-- -----------------------------------------------------------
-- 3. 站点数据查询
-- 站点坐标落在 0.25° 网格上，直接通过 lat/lon 关联 forecast_grid 或
-- observation_data 查询，不需要额外建表。
-- -----------------------------------------------------------


-- ============================================================
-- 四、模型 & 版本管理模块
-- model = 模型定义（原 model_group，重命名）
-- model_version = 模型版本（原 forecast_model，重命名 + 去冗余）
-- ============================================================

-- -----------------------------------------------------------
-- 1. 模型表
-- -----------------------------------------------------------
CREATE TABLE model (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    model_name  VARCHAR(100) NOT NULL COMMENT '模型名称',
    model_type  VARCHAR(50)  NOT NULL COMMENT 'SST/CHL/CUSTOM',
    description TEXT         DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_model_name (model_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 2. 模型版本表
-- model 和 model_version 是一对多关系
-- model_type 从版本表移除，统一查 model 表获取
-- -----------------------------------------------------------
CREATE TABLE model_version (
    id               BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    model_id         BIGINT       NOT NULL COMMENT 'FK → model.id',
    version_label    VARCHAR(20)  NOT NULL DEFAULT 'v1' COMMENT 'v1/v2/...',
    params_config    TEXT         DEFAULT NULL COMMENT '模型参数 JSON',
    cron_expression  VARCHAR(50)  DEFAULT '' COMMENT '调度 cron',
    data_source      VARCHAR(500) DEFAULT NULL COMMENT '训练/输入数据来源描述',
    data_time_range  VARCHAR(200) DEFAULT NULL COMMENT '训练数据时间范围',
    change_note      TEXT         DEFAULT NULL COMMENT '版本变更说明',
    status           VARCHAR(20)  NOT NULL DEFAULT 'STOPPED' COMMENT 'RUNNING/STOPPED/ERROR',
    last_run_time    DATETIME     DEFAULT NULL,
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_model_version (model_id, version_label),
    INDEX idx_model_status (model_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 3. 外键关系
-- model.id  ←→  model_version.model_id
-- model_version.id  ←→  forecast_grid.version_id
-- forecast_grid.model_id  ←→  model.id（冗余，避免 JOIN）
-- -----------------------------------------------------------

-- -----------------------------------------------------------
-- 4. 常见查询
--
-- 运行概览（Dashboard 统计）：
-- SELECT m.model_name, mv.version_label, mv.status, mv.last_run_time
-- FROM model m
-- JOIN model_version mv ON mv.model_id = m.id
-- WHERE mv.status = 'RUNNING';
--
-- 模型分页列表（含版本数、运行数）：
-- SELECT m.*,
--        COUNT(mv.id) AS version_count,
--        SUM(mv.status = 'RUNNING') AS running_count
-- FROM model m
-- LEFT JOIN model_version mv ON mv.model_id = m.id
-- GROUP BY m.id;
--
-- 启动/停止版本：
-- UPDATE model_version SET status = 'RUNNING' WHERE id = ?;
--
-- 预测结果追溯（从预报数据查模型）：
-- SELECT m.model_name, mv.version_label, fg.*
-- FROM forecast_grid fg
-- JOIN model_version mv ON mv.id = fg.version_id
-- JOIN model m ON m.id = fg.model_id
-- WHERE fg.forecast_date = '2026-05-15';
-- -----------------------------------------------------------


-- ============================================================
-- 五、告警模块
-- 区域聚合卡片 + 站点明细下钻
-- ============================================================

-- -----------------------------------------------------------
-- 1. 告警规则表
-- 替代 Controller 里硬编码的阈值
-- -----------------------------------------------------------
CREATE TABLE alert_rule (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    rule_name   VARCHAR(100) NOT NULL COMMENT '规则名称',
    variable    VARCHAR(10)  NOT NULL COMMENT '变量: sst/chl',
    source      VARCHAR(20)  NOT NULL DEFAULT 'forecast' COMMENT '数据来源: forecast/observation',
    operator    VARCHAR(5)   NOT NULL DEFAULT '>' COMMENT '比较符: >/>=/</<=',
    threshold   DOUBLE       NOT NULL COMMENT '阈值',
    severity    VARCHAR(10)  NOT NULL DEFAULT 'warning' COMMENT 'info/warning/critical',
    is_active   TINYINT      NOT NULL DEFAULT 1,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_var_source (variable, source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始规则：
INSERT INTO alert_rule (rule_name, variable, source, operator, threshold, severity) VALUES
('SST 高温告警', 'sst', 'forecast', '>', 28.0, 'warning'),
('CHL 高浓度告警', 'chl', 'forecast', '>', 5.0, 'warning');

-- -----------------------------------------------------------
-- 2. 告警事件表（区域级别聚合卡片）
-- 每条 = 一个健康区域 × 一个变量 × 一天
-- -----------------------------------------------------------
CREATE TABLE alert_event (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    zone_id       BIGINT       NOT NULL COMMENT 'FK → health_zone.id',
    rule_id       BIGINT       DEFAULT NULL COMMENT 'FK → alert_rule.id',
    variable      VARCHAR(10)  NOT NULL,
    source        VARCHAR(20)  NOT NULL COMMENT 'forecast/observation',
    alert_date    DATE         NOT NULL COMMENT '告警数据日期',
    max_value     DOUBLE       NOT NULL COMMENT '区域内最高值',
    avg_value     DOUBLE       NOT NULL COMMENT '区域均值',
    threshold     DOUBLE       NOT NULL COMMENT '触发阈值',
    station_count INT          NOT NULL DEFAULT 0 COMMENT '超标站点数',
    severity      VARCHAR(10)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'active' COMMENT 'active/acknowledged/resolved',
    ack_by        BIGINT       DEFAULT NULL COMMENT 'FK → sys_user.id',
    ack_at        DATETIME     DEFAULT NULL,
    message       VARCHAR(200) DEFAULT NULL COMMENT '如: 北部近岸 SST 超标，3 个站点受影响',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_zone_date_var (zone_id, alert_date, variable, rule_id),
    INDEX idx_date_stat (alert_date, status),
    INDEX idx_zone (zone_id, alert_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 3. 告警站点明细表（区域卡片的下钻数据）
-- 每条 = 一个告警区域内的一个超标站点
-- ⚠️ 一致性约束（应用层保证）：
--    alert_event.zone_id = monitoring_station.health_zone_id
--    即告警区域必须和站点的所属健康区域一致
-- -----------------------------------------------------------
CREATE TABLE alert_station_detail (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    alert_id      BIGINT       NOT NULL COMMENT 'FK → alert_event.id',
    station_id    BIGINT       NOT NULL COMMENT 'FK → monitoring_station.id',
    actual_value  DOUBLE       NOT NULL COMMENT '站点实际值',
    threshold     DOUBLE       NOT NULL,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_alert_station (alert_id, station_id),
    INDEX idx_alert (alert_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 4. 告警生成（定时任务 / 模型跑完触发）
--
-- Step 1：按区域聚合，生成 alert_event
-- INSERT INTO alert_event (zone_id, rule_id, variable, source, alert_date,
--                          max_value, avg_value, threshold, station_count, severity, message)
-- SELECT hz.id, r.id, r.variable, r.source, fg.forecast_date,
--        MAX(fg.value), AVG(fg.value), r.threshold,
--        COUNT(DISTINCT ms.id), r.severity,
--        CONCAT(hz.zone_name, ' ', r.variable, ' 超标，',
--               COUNT(DISTINCT ms.id), ' 个站点受影响')
-- FROM forecast_grid fg
-- JOIN health_zone hz ON fg.lat BETWEEN hz.min_lat AND hz.max_lat
--                     AND fg.lon BETWEEN hz.min_lon AND hz.max_lon
-- JOIN alert_rule r ON r.variable = fg.variable AND r.is_active = 1
--                   AND fg.value > r.threshold
-- LEFT JOIN monitoring_station ms ON ms.lat = fg.lat AND ms.lon = fg.lon
-- WHERE fg.forecast_date = ?
-- GROUP BY hz.id, r.id, fg.forecast_date
-- ON DUPLICATE KEY UPDATE max_value = VALUES(max_value), ...
--
-- Step 2：写入超标站点明细
-- INSERT INTO alert_station_detail (alert_id, station_id, actual_value, threshold)
-- SELECT ae.id, ms.id, fg.value, ae.threshold
-- FROM alert_event ae
-- JOIN health_zone hz ON hz.id = ae.zone_id
-- JOIN forecast_grid fg ON fg.variable = ae.variable
--                       AND fg.forecast_date = ae.alert_date
--                       AND fg.lat BETWEEN hz.min_lat AND hz.max_lat
--                       AND fg.lon BETWEEN hz.min_lon AND hz.max_lon
--                       AND fg.value > ae.threshold
-- JOIN monitoring_station ms ON ms.lat = fg.lat AND ms.lon = fg.lon
-- WHERE ae.alert_date = ?
-- ON DUPLICATE KEY UPDATE actual_value = VALUES(actual_value);
-- -----------------------------------------------------------

-- -----------------------------------------------------------
-- 5. 前端查询
--
-- Dashboard 告警卡片：
-- SELECT ae.*, hz.zone_name
-- FROM alert_event ae
-- JOIN health_zone hz ON hz.id = ae.zone_id
-- WHERE ae.alert_date = CURDATE() AND ae.status = 'active'
-- ORDER BY ae.severity DESC;
--
-- 点击卡片 → 下钻站点列表：
-- SELECT ms.station_name, ms.lat, ms.lon, asd.actual_value, asd.threshold
-- FROM alert_station_detail asd
-- JOIN monitoring_station ms ON ms.id = asd.station_id
-- WHERE asd.alert_id = ?
-- ORDER BY asd.actual_value DESC;
-- -----------------------------------------------------------


-- ============================================================
-- 六、健康评估模块
-- 按区域评估 SST/CHL/热浪 三项指标，综合打分
-- 可选留存历史，支持趋势对比
-- ============================================================

-- -----------------------------------------------------------
-- 1. 健康评估区域表
-- 每个区域是一个矩形 bbox，可和海域、站点分类交叉
-- -----------------------------------------------------------
CREATE TABLE health_zone (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    zone_name  VARCHAR(100) NOT NULL COMMENT '区域名称',
    min_lon    DECIMAL(9,6) NOT NULL,
    max_lon    DECIMAL(9,6) NOT NULL,
    min_lat    DECIMAL(8,6) NOT NULL,
    max_lat    DECIMAL(8,6) NOT NULL,
    sort_order INT          NOT NULL DEFAULT 0,
    is_active  TINYINT      NOT NULL DEFAULT 1,
    UNIQUE KEY uk_zone_name (zone_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 初始区域示例（可按需调整）：
INSERT INTO health_zone (zone_name, min_lon, max_lon, min_lat, max_lat, sort_order) VALUES
('北部近岸', 121.33, 122.50, 30.00, 32.67, 1),
('北部远海', 122.50, 125.58, 30.00, 32.67, 2),
('南部近岸', 121.33, 122.50, 26.92, 30.00, 3),
('南部远海', 122.50, 125.58, 26.92, 30.00, 4);

-- -----------------------------------------------------------
-- 2. 健康评估记录表（留存历史，支持环比/同比对比）
-- -----------------------------------------------------------
CREATE TABLE health_record (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    zone_id         BIGINT       NOT NULL COMMENT 'FK → health_zone.id',
    assess_date     DATE         NOT NULL,

    -- SST 指标
    sst_avg         DOUBLE       DEFAULT NULL COMMENT '区域 SST 均值',
    sst_max         DOUBLE       DEFAULT NULL COMMENT '区域 SST 最高值',
    sst_anomaly     DOUBLE       DEFAULT NULL COMMENT 'SST 距平（相对气候态）',
    sst_trend       VARCHAR(20)  DEFAULT NULL COMMENT 'rising/falling/stable',

    -- CHL 指标
    chl_avg         DOUBLE       DEFAULT NULL,
    chl_max         DOUBLE       DEFAULT NULL,
    chl_trend       VARCHAR(20)  DEFAULT NULL,

    -- 热浪指标
    heatwave_active TINYINT      DEFAULT 0  COMMENT '是否处于热浪期',
    heatwave_days   INT          DEFAULT 0  COMMENT '连续热浪天数',

    -- 综合评级
    sst_grade       VARCHAR(10)  DEFAULT NULL COMMENT 'good/fine/warn/bad',
    chl_grade       VARCHAR(10)  DEFAULT NULL,
    heatwave_grade  VARCHAR(10)  DEFAULT NULL,
    overall_grade   VARCHAR(10)  DEFAULT NULL COMMENT 'good/fine/warn/bad（取三者最差）',

    suggestions     TEXT         DEFAULT NULL COMMENT '风险评估文字建议',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE KEY uk_zone_date (zone_id, assess_date),
    INDEX idx_date (assess_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -----------------------------------------------------------
-- 3. 健康评估计算逻辑（定时任务 / 按需触发）
--
-- 伪 SQL（具体计算在 Java 层做更合适）：
--
-- 对于每个 health_zone：
--   -- SST 均值、最大值
--   SELECT AVG(value), MAX(value)
--   FROM forecast_grid
--   WHERE variable = 'sst' AND forecast_date = ?
--     AND lat BETWEEN zone.min_lat AND zone.max_lat
--     AND lon BETWEEN zone.min_lon AND zone.max_lon;
--
--   -- SST 距平（需要气候态参考，简化为对比前 30 天均值）
--   SST_anomaly = sst_avg - AVG(sst_avg over past 30 days)
--
--   -- SST 趋势（对比前 7 天）
--   CASE WHEN sst_avg > prev_7day_avg + 0.5 THEN 'rising'
--        WHEN sst_avg < prev_7day_avg - 0.5 THEN 'falling'
--        ELSE 'stable'
--
--   -- 热浪检测：SST 连续 ≥ 5 天超过阈值 → active
--
--   -- 评级（阈值来自 health-assessment.js）：
--   SST: anomaly > 2.5 → bad, > 1.5 → warn, > 0.5 → fine, else → good
--   CHL: avg >= 5.0 → bad, >= 3.0 → warn, >= 2.0 → fine, else → good
--   Heatwave: active → bad, else → good
--   Overall: worst(sst_grade, chl_grade, heatwave_grade)
-- -----------------------------------------------------------

-- -----------------------------------------------------------
-- 4. 前端查询
--
-- 当前健康概览（Dashboard）：
-- SELECT hr.*, hz.zone_name
-- FROM health_record hr
-- JOIN health_zone hz ON hz.id = hr.zone_id
-- WHERE hr.assess_date = CURDATE();
--
-- 环比对比（今天 vs 昨天，哪些区域恶化了）：
-- SELECT hz.zone_name,
--        hr1.overall_grade AS today_grade,
--        hr2.overall_grade AS yesterday_grade
-- FROM health_zone hz
-- JOIN health_record hr1 ON hr1.zone_id = hz.id AND hr1.assess_date = CURDATE()
-- JOIN health_record hr2 ON hr2.zone_id = hz.id AND hr2.assess_date = DATE_SUB(CURDATE(), INTERVAL 1 DAY)
-- WHERE hr1.overall_grade <> hr2.overall_grade;
--
-- 某区域近期趋势（OceanHealthView 详情）：
-- SELECT assess_date, sst_avg, sst_anomaly, chl_avg,
--        heatwave_active, heatwave_days, overall_grade
-- FROM health_record
-- WHERE zone_id = ? AND assess_date BETWEEN ? AND ?
-- ORDER BY assess_date;
-- -----------------------------------------------------------
