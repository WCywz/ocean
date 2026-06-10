package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.ModelScheduleSaveDTO;
import com.ocean.service.ModelScheduleService;
import com.ocean.vo.ModelScheduleVO;
import com.ocean.vo.VersionCardVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/model")
public class ModelScheduleController {

    @Autowired
    private ModelScheduleService scheduleService;

    @GetMapping("/{modelId}/version/{versionId}/schedule")
    public Result<List<ModelScheduleVO>> getVersionSchedules(
            @PathVariable Long modelId,
            @PathVariable Long versionId) {
        return Result.success(scheduleService.getSchedulesByVersionId(versionId));
    }

    @PostMapping("/{modelId}/version/{versionId}/schedule")
    public Result<ModelScheduleVO> addSchedule(
            @PathVariable Long modelId,
            @PathVariable Long versionId,
            @RequestBody ModelScheduleSaveDTO dto) {
        dto.setVersionId(versionId);
        return Result.success(scheduleService.addSchedule(versionId, dto));
    }

    @PutMapping("/schedule/{id}")
    public Result<?> updateSchedule(
            @PathVariable Long id,
            @RequestBody ModelScheduleSaveDTO dto) {
        scheduleService.updateSchedule(id, dto);
        return Result.success("调度更新成功");
    }

    @DeleteMapping("/schedule/{id}")
    public Result<?> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return Result.success("调度已删除");
    }

    @GetMapping("/schedule/week")
    public Result<List<ModelScheduleVO>> getWeekSchedules(
            @RequestParam(required = false) Long modelId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return Result.success(scheduleService.getWeekSchedules(modelId, startDate, endDate));
    }

    @GetMapping("/schedule/available-versions")
    public Result<List<VersionCardVO>> getAvailableVersions() {
        return Result.success(scheduleService.getAvailableVersionsForSchedule());
    }
}
