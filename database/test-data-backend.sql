-- Test data for backend rewrite
-- Models
INSERT INTO model (model_name, model_type, description) VALUES
('SST 海表温度预报模型', 'SST', '基于ROMS+WRF的东海SST预报'),
('CHL 叶绿素浓度预报模型', 'CHL', '基于ROMS+NPCZ的东海CHL预报');

INSERT INTO model_version (model_id, version_label, params_config, cron_expression, data_source, data_time_range, change_note, status) VALUES
(1, 'v1', '{"layers":3,"hidden":64}', '0 6 * * *', 'HYCOM+NCEP GDAS', '2024-01-01 ~ 2025-12-31', '初始版本', 'RUNNING'),
(2, 'v1', '{"layers":3,"hidden":64}', '0 6 * * *', 'HYCOM+NCEP GDAS', '2024-01-01 ~ 2025-12-31', '初始版本', 'RUNNING');

-- Insert forecast grid data for SST (2026-05-15)
INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 1, 1, 'sst', '2026-05-15', 0,
       ROUND(26.75 + 0.25 * lat_idx, 6),
       ROUND(121.25 + 0.25 * lon_idx, 6),
       ROUND(18.0 + RAND(42+lat_idx*100+lon_idx) * 10.0, 3),
       'degree_C'
FROM (SELECT 0 AS lat_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23) AS lats
CROSS JOIN (SELECT 0 AS lon_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17) AS lons
WHERE ROUND(26.75 + 0.25 * lat_idx, 6) BETWEEN 26.92 AND 32.67
  AND ROUND(121.25 + 0.25 * lon_idx, 6) BETWEEN 121.33 AND 125.58;

-- CHL forecast for same date
INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 2, 2, 'chl', '2026-05-15', 0,
       ROUND(26.75 + 0.25 * lat_idx, 6),
       ROUND(121.25 + 0.25 * lon_idx, 6),
       ROUND(0.5 + RAND(100+lat_idx*100+lon_idx) * 6.0, 3),
       'mg_m3'
FROM (SELECT 0 AS lat_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17 UNION SELECT 18 UNION SELECT 19 UNION SELECT 20 UNION SELECT 21 UNION SELECT 22 UNION SELECT 23) AS lats
CROSS JOIN (SELECT 0 AS lon_idx UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16 UNION SELECT 17) AS lons
WHERE ROUND(26.75 + 0.25 * lat_idx, 6) BETWEEN 26.92 AND 32.67
  AND ROUND(121.25 + 0.25 * lon_idx, 6) BETWEEN 121.33 AND 125.58;

-- Historic forecast data (7 days back)
INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 1, 1, 'sst', DATE_ADD('2026-05-15', INTERVAL -d DAY), 0,
       ROUND(26.75 + 0.25 * FLOOR(RAND(200+d) * 24), 6),
       ROUND(121.25 + 0.25 * FLOOR(RAND(300+d) * 18), 6),
       ROUND(18.0 + RAND(500+d) * 10.0, 3),
       'degree_C'
FROM (SELECT 1 AS d UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) AS days;

INSERT INTO forecast_grid (model_id, version_id, variable, forecast_date, depth, lat, lon, value, unit)
SELECT 2, 2, 'chl', DATE_ADD('2026-05-15', INTERVAL -d DAY), 0,
       ROUND(26.75 + 0.25 * FLOOR(RAND(200+d) * 24), 6),
       ROUND(121.25 + 0.25 * FLOOR(RAND(300+d) * 18), 6),
       ROUND(0.5 + RAND(500+d) * 6.0, 3),
       'mg_m3'
FROM (SELECT 1 AS d UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7) AS days;

-- Monitoring stations
INSERT INTO monitoring_station (station_name, lat, lon, distance, region, health_zone_id) VALUES
('舟山近岸站', 29.9875, 122.2075, '近岸', '北部', 1),
('宁波近岸站', 29.8625, 121.5725, '近岸', '北部', 1),
('长江口站', 31.2500, 122.0000, '近海', '北部', 1),
('舟山东部站', 29.7500, 123.5000, '远海', '北部', 2),
('温州近岸站', 27.8350, 121.1250, '近岸', '南部', 3),
('钓鱼岛西站', 27.0000, 124.0000, '远海', '南部', 4);

-- Observation data samples
INSERT INTO observation_data (variable, obs_time, depth, lat, lon, value) VALUES
('thetao', '2026-05-15', 0, 29.9875, 122.2075, 23.5),
('thetao', '2026-05-14', 0, 29.9875, 122.2075, 23.2),
('thetao', '2026-05-13', 0, 29.9875, 122.2075, 22.8),
('chl', '2026-05-15', 0, 29.9875, 122.2075, 2.1),
('chl', '2026-05-14', 0, 29.9875, 122.2075, 1.9),
('so', '2026-05-15', 0, 29.9875, 122.2075, 32.5),
('thetao', '2026-05-15', 10, 29.9875, 122.2075, 22.0),
('thetao', '2026-05-15', 20, 29.9875, 122.2075, 20.5);

-- Grid cache
INSERT INTO observation_grid_cache (lat, lon)
SELECT DISTINCT ROUND(lat, 6), ROUND(lon, 6) FROM forecast_grid;

-- Health records for 2026-05-15
INSERT INTO health_record (zone_id, assess_date, sst_avg, sst_max, sst_anomaly, sst_trend, chl_avg, chl_max, chl_trend, heatwave_active, heatwave_days, sst_grade, chl_grade, heatwave_grade, overall_grade) VALUES
(1, '2026-05-15', 21.5, 23.8, 0.8, 'stable', 2.1, 3.5, 'stable', 0, 0, 'fine', 'fine', 'good', 'fine'),
(2, '2026-05-15', 22.3, 24.5, 1.2, 'rising', 1.5, 2.8, 'stable', 0, 0, 'warn', 'good', 'good', 'warn'),
(3, '2026-05-15', 23.1, 25.0, 0.5, 'stable', 2.8, 4.2, 'rising', 0, 0, 'fine', 'fine', 'good', 'fine'),
(4, '2026-05-15', 23.8, 26.2, 1.8, 'rising', 1.2, 2.0, 'stable', 0, 0, 'warn', 'good', 'good', 'warn');
