-- 模型运行日志表
CREATE TABLE IF NOT EXISTS model_run_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    version_id BIGINT NOT NULL COMMENT '版本ID',
    model_id BIGINT NOT NULL COMMENT '模型ID（冗余，方便查询）',
    model_name VARCHAR(100) COMMENT '模型名称（冗余，方便查询）',
    version_label VARCHAR(20) COMMENT '版本号（冗余，方便查询）',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms BIGINT DEFAULT 0 COMMENT '耗时（毫秒）',
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT 'SUCCESS / FAILED / RUNNING',
    error_message TEXT COMMENT '错误信息',
    output_summary VARCHAR(500) COMMENT '输出摘要（如：生成7200个格点）',
    log_text TEXT COMMENT '完整执行日志',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_version_id (version_id),
    INDEX idx_model_id (model_id),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模型运行日志';
