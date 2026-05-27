package com.ocean.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.entity.ForecastGrid;
import com.ocean.entity.HealthRecord;
import com.ocean.entity.ObservationData;
import com.ocean.entity.ObservationGrid;
import com.ocean.mapper.ForecastGridMapper;
import com.ocean.mapper.HealthRecordMapper;
import com.ocean.mapper.ObservationDataMapper;
import com.ocean.mapper.ObservationGridMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 流水线串行化锁 + 数据状态查询。
 * 所有流水线入口（SystemDateTask、三个兜底 cron、StartupCatchUpTask）
 * 在执行前 tryLock，保证同一时刻只有一个流水线在跑。
 */
@Slf4j
@Service
public class PipelineLockService {

    private final ReentrantLock lock = new ReentrantLock();

    @Autowired private ObservationDataMapper observationDataMapper;
    @Autowired private ObservationGridMapper observationGridMapper;
    @Autowired private ForecastGridMapper forecastGridMapper;
    @Autowired private HealthRecordMapper healthRecordMapper;

    public boolean tryLock() {
        boolean acquired = lock.tryLock();
        if (!acquired) {
            log.info("流水线锁被占用，跳过本次执行");
        }
        return acquired;
    }

    public void unlock() {
        lock.unlock();
    }

    public boolean isObservationIngested(LocalDate date) {
        Long dataCount = observationDataMapper.selectCount(
                new LambdaQueryWrapper<ObservationData>()
                        .eq(ObservationData::getObsTime, date)
                        .last("LIMIT 1"));
        Long gridCount = observationGridMapper.selectCount(
                new LambdaQueryWrapper<ObservationGrid>()
                        .eq(ObservationGrid::getObsDate, date)
                        .last("LIMIT 1"));
        return dataCount != null && dataCount > 0 && gridCount != null && gridCount > 0;
    }

    public boolean isForecastComplete(LocalDate date) {
        Long count = forecastGridMapper.selectCount(
                new LambdaQueryWrapper<ForecastGrid>()
                        .eq(ForecastGrid::getForecastDate, date)
                        .last("LIMIT 1"));
        return count != null && count > 0;
    }

    public boolean isHealthAssessed(LocalDate date) {
        Long count = healthRecordMapper.selectCount(
                new LambdaQueryWrapper<HealthRecord>()
                        .eq(HealthRecord::getAssessDate, date)
                        .last("LIMIT 1"));
        return count != null && count > 0;
    }
}
