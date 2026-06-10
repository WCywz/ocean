package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.entity.ModelRunLog;
import com.ocean.mapper.ModelRunLogMapper;
import com.ocean.service.ModelRunLogService;
import com.ocean.vo.RunLogVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModelRunLogServiceImpl implements ModelRunLogService {

    @Autowired
    private ModelRunLogMapper runLogMapper;

    @Override
    public IPage<RunLogVO> getLogPage(Integer pageNum, Integer pageSize, Long versionId) {
        Page<ModelRunLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ModelRunLog> wrapper = new LambdaQueryWrapper<>();
        if (versionId != null) wrapper.eq(ModelRunLog::getVersionId, versionId);
        wrapper.orderByDesc(ModelRunLog::getStartTime);
        return runLogMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    public RunLogVO getLogById(Long id) {
        ModelRunLog log = runLogMapper.selectById(id);
        return log == null ? null : toVO(log);
    }

    @Override
    public Map<String, Object> getTodayOverview() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<ModelRunLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(ModelRunLog::getStartTime, todayStart);

        List<ModelRunLog> todayLogs = runLogMapper.selectList(wrapper);
        Map<String, Object> result = new HashMap<>();
        result.put("total", todayLogs.size());
        result.put("success", todayLogs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count());
        result.put("failed", todayLogs.stream().filter(l -> "FAILED".equals(l.getStatus())).count());
        result.put("running", todayLogs.stream().filter(l -> "RUNNING".equals(l.getStatus())).count());
        return result;
    }

    @Override
    public List<RunLogVO> getRecentLogs() {
        // Latest one log per version, ordered by start_time desc
        LambdaQueryWrapper<ModelRunLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ModelRunLog::getStartTime);
        List<ModelRunLog> all = runLogMapper.selectList(wrapper);

        Map<Long, RunLogVO> latest = new LinkedHashMap<>();
        for (ModelRunLog log : all) {
            latest.putIfAbsent(log.getVersionId(), toVO(log));
        }
        return new ArrayList<>(latest.values());
    }

    @Override
    public List<RunLogVO> getHistory(Long versionId, Integer days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days != null ? days : 7);
        LambdaQueryWrapper<ModelRunLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelRunLog::getVersionId, versionId)
               .ge(ModelRunLog::getStartTime, since)
               .orderByDesc(ModelRunLog::getStartTime);
        return runLogMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public String exportCsv(Long versionId, String start, String end) {
        LambdaQueryWrapper<ModelRunLog> wrapper = new LambdaQueryWrapper<>();
        if (versionId != null) wrapper.eq(ModelRunLog::getVersionId, versionId);
        if (start != null && !start.isEmpty()) wrapper.ge(ModelRunLog::getStartTime, LocalDateTime.parse(start));
        if (end != null && !end.isEmpty()) wrapper.le(ModelRunLog::getStartTime, LocalDateTime.parse(end));
        wrapper.orderByDesc(ModelRunLog::getStartTime);
        List<ModelRunLog> logs = runLogMapper.selectList(wrapper);

        StringBuilder sb = new StringBuilder();
        sb.append("日期,时间,版本,状态,耗时,错误信息\n");
        for (ModelRunLog log : logs) {
            String date = log.getStartTime() != null ? log.getStartTime().toLocalDate().toString() : "-";
            String time = log.getStartTime() != null ? log.getStartTime().toLocalTime().toString() : "-";
            String duration = log.getDurationMs() != null ? log.getDurationMs() / 1000 + "s" : "-";
            String error = log.getErrorMessage() != null ? log.getErrorMessage().replace(",", "，") : "";
            sb.append(String.format("%s,%s,%s %s,%s,%s,%s\n",
                    date, time,
                    log.getModelName() != null ? log.getModelName() : "",
                    log.getVersionLabel() != null ? log.getVersionLabel() : "",
                    log.getStatus(), duration, error));
        }
        return sb.toString();
    }

    private RunLogVO toVO(ModelRunLog log) {
        RunLogVO vo = new RunLogVO();
        BeanUtils.copyProperties(log, vo);
        return vo;
    }
}
