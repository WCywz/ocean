package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.vo.RunLogVO;

import java.util.List;
import java.util.Map;

public interface ModelRunLogService {

    IPage<RunLogVO> getLogPage(Integer pageNum, Integer pageSize, Long versionId);

    RunLogVO getLogById(Long id);

    Map<String, Object> getTodayOverview();

    List<RunLogVO> getRecentLogs();

    List<RunLogVO> getHistory(Long versionId, Integer days);

    String exportCsv(Long versionId, String start, String end);
}
