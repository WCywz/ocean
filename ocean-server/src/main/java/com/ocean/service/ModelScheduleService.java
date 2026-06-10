package com.ocean.service;

import com.ocean.dto.ModelScheduleSaveDTO;
import com.ocean.vo.ModelScheduleVO;
import com.ocean.vo.VersionCardVO;

import java.time.LocalDate;
import java.util.List;

public interface ModelScheduleService {
    List<ModelScheduleVO> getSchedulesByVersionId(Long versionId);
    ModelScheduleVO addSchedule(Long versionId, ModelScheduleSaveDTO dto);
    void updateSchedule(Long scheduleId, ModelScheduleSaveDTO dto);
    void deleteSchedule(Long scheduleId);
    List<ModelScheduleVO> getWeekSchedules(Long modelId, LocalDate startDate, LocalDate endDate);
    List<VersionCardVO> getAvailableVersionsForSchedule();
}
