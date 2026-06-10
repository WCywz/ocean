CREATE TABLE IF NOT EXISTS model_schedule (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id     BIGINT NOT NULL COMMENT '版本ID',
    schedule_label VARCHAR(50) COMMENT '调度标签',
    repetition     VARCHAR(20) NOT NULL COMMENT 'DAILY / WEEKLY / ONCE',
    day_of_week    INT COMMENT 'WEEKLY时: 1=周一..7=周日',
    schedule_time  TIME NOT NULL COMMENT '调度时间 HH:mm',
    is_active      TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_version_id (version_id),
    FOREIGN KEY (version_id) REFERENCES model_version(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型调度配置';
