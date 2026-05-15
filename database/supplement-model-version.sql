-- ============================================
-- 模型版本分层 - 数据库补充脚本
-- 将扁平 forecast_model 表改造为 model_group + forecast_model(版本) 两层结构
-- ============================================

USE ocean_forecast;

-- ============================================
-- 1. 创建模型组表
-- ============================================
CREATE TABLE IF NOT EXISTS model_group (
    id          BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键ID',
    model_name  VARCHAR(100) NOT NULL                  COMMENT '模型名称',
    model_type  VARCHAR(50)  NOT NULL                  COMMENT '模型类型',
    description TEXT         DEFAULT NULL              COMMENT '模型介绍：功能、适用场景、方法论等',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预报模型组表';

-- ============================================
-- 2. 为 forecast_model 添加版本相关字段
-- ============================================
ALTER TABLE forecast_model
    ADD COLUMN IF NOT EXISTS group_id        BIGINT       DEFAULT NULL COMMENT '关联模型组ID' AFTER id,
    ADD COLUMN IF NOT EXISTS version_label   VARCHAR(20)  DEFAULT 'v1' COMMENT '版本号标识（v1, v2, ...）',
    ADD COLUMN IF NOT EXISTS data_source     VARCHAR(500) DEFAULT NULL COMMENT '训练数据来源',
    ADD COLUMN IF NOT EXISTS data_time_range VARCHAR(200) DEFAULT NULL COMMENT '数据时间范围',
    ADD COLUMN IF NOT EXISTS change_note     TEXT         DEFAULT NULL COMMENT '变更说明（相对上一版本）';

-- 添加外键索引
CREATE INDEX IF NOT EXISTS idx_group_id ON forecast_model (group_id);

-- ============================================
-- 3. 数据迁移：将现有模型迁移到两层结构
-- ============================================

-- 从现有模型名称中提取基础名称（去掉空格后版本后缀），为每个基础名称创建模型组
INSERT INTO model_group (model_name, model_type, description, create_time, update_time)
SELECT DISTINCT
    TRIM(REPLACE(REPLACE(REPLACE(model_name, ' v1', ''), ' v2', ''), ' v3', '')) AS base_name,
    model_type,
    description,
    MIN(create_time) AS create_time,
    MAX(update_time) AS update_time
FROM forecast_model
WHERE group_id IS NULL
GROUP BY base_name, model_type, description;

-- 更新 forecast_model，关联到对应的模型组
UPDATE forecast_model fm
JOIN model_group mg ON mg.model_name = TRIM(REPLACE(REPLACE(REPLACE(fm.model_name, ' v1', ''), ' v2', ''), ' v3', ''))
SET fm.group_id = mg.id;

-- 从原始模型名称中提取版本号作为 version_label
UPDATE forecast_model
SET version_label = CASE
    WHEN model_name LIKE '% v3' THEN 'v3'
    WHEN model_name LIKE '% v2' THEN 'v2'
    WHEN model_name LIKE '% v1' THEN 'v1'
    ELSE 'v1'
END
WHERE version_label = 'v1' AND group_id IS NOT NULL;
