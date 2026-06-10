-- 删除未使用的告警相关表
-- alert_event, alert_rule, alert_station_detail 无任何后端/前端代码引用，属于历史遗留

DROP TABLE IF EXISTS alert_station_detail;
DROP TABLE IF EXISTS alert_event;
DROP TABLE IF EXISTS alert_rule;
