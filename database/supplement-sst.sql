-- Supplement: add SST (sea surface temperature) column to ocean_data
ALTER TABLE ocean_data
    ADD COLUMN sst DOUBLE DEFAULT NULL COMMENT '海表温度 (°C)'
    AFTER chl;

-- Seed SST data for existing records
-- 渤海 (38.5, 119.5): cooler, ~15-16°C
UPDATE ocean_data SET sst = 16.2 WHERE lat = 38.500000 AND lon = 119.500000 AND time = '2026-04-28';
UPDATE ocean_data SET sst = 16.5 WHERE lat = 38.500000 AND lon = 119.500000 AND time = '2026-04-29';
UPDATE ocean_data SET sst = 16.8 WHERE lat = 38.500000 AND lon = 119.500000 AND time = '2026-04-30';

-- 黄海 (36.0, 122.5): moderate, ~17-18°C
UPDATE ocean_data SET sst = 17.8 WHERE lat = 36.000000 AND lon = 122.500000 AND time = '2026-04-28';
UPDATE ocean_data SET sst = 18.1 WHERE lat = 36.000000 AND lon = 122.500000 AND time = '2026-04-29';
UPDATE ocean_data SET sst = 18.4 WHERE lat = 36.000000 AND lon = 122.500000 AND time = '2026-04-30';

-- 东海 (30.0, 124.0): warmer, ~22-24°C
UPDATE ocean_data SET sst = 22.8 WHERE lat = 30.000000 AND lon = 124.000000 AND time = '2026-04-28';
UPDATE ocean_data SET sst = 23.1 WHERE lat = 30.000000 AND lon = 124.000000 AND time = '2026-04-29';
UPDATE ocean_data SET sst = 23.5 WHERE lat = 30.000000 AND lon = 124.000000 AND time = '2026-04-30';
