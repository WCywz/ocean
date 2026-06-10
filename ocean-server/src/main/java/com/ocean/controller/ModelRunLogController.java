package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.service.ModelRunLogService;
import com.ocean.vo.RunLogVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/model/run-log")
public class ModelRunLogController {

    @Autowired
    private ModelRunLogService runLogService;

    @GetMapping("/page")
    public Result<IPage<RunLogVO>> getLogPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long versionId) {
        return Result.success(runLogService.getLogPage(pageNum, pageSize, versionId));
    }

    @GetMapping("/{id}")
    public Result<RunLogVO> getLogById(@PathVariable Long id) {
        RunLogVO vo = runLogService.getLogById(id);
        return vo != null ? Result.success(vo) : Result.error("日志不存在");
    }

    @GetMapping("/today-overview")
    public Result<Map<String, Object>> getTodayOverview() {
        return Result.success(runLogService.getTodayOverview());
    }

    @GetMapping("/recent")
    public Result<List<RunLogVO>> getRecentLogs() {
        return Result.success(runLogService.getRecentLogs());
    }

    @GetMapping("/history")
    public Result<List<RunLogVO>> getHistory(
            @RequestParam Long versionId,
            @RequestParam(defaultValue = "7") Integer days) {
        return Result.success(runLogService.getHistory(versionId, days));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) Long versionId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        String csv = runLogService.exportCsv(versionId, start, end);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", "run_log_export.csv");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}
