-- 模型告警事件表
CREATE TABLE IF NOT EXISTS alert_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL COMMENT '版本ID',
    model_id BIGINT NOT NULL COMMENT '模型ID',
    model_name VARCHAR(100) COMMENT '模型名称',
    version_label VARCHAR(20) COMMENT '版本号',
    alert_type VARCHAR(30) NOT NULL COMMENT 'EXECUTION_FAILED / CONSECUTIVE_FAILURES / EXECUTION_TIMEOUT',
    message VARCHAR(500) COMMENT '告警消息',
    run_log_id BIGINT COMMENT '关联的运行日志ID',
    is_read TINYINT DEFAULT 0 COMMENT '是否已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_version_id (version_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型告警事件';
