-- 为 ONCE 调度补充日期字段
ALTER TABLE model_schedule
    ADD COLUMN schedule_date DATE NULL COMMENT 'ONCE调度指定日期' AFTER schedule_time;
