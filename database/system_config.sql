-- 系统配置表
CREATE TABLE system_config (
    config_key   VARCHAR(50) NOT NULL PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO system_config (config_key, config_value) VALUES ('system_date', '2026-01-01');
