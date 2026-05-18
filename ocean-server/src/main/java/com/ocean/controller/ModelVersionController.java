package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.service.ModelVersionService;
import com.ocean.vo.ModelVersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model/{modelId}/version")
public class ModelVersionController {

    @Autowired
    private ModelVersionService modelVersionService;

    @GetMapping
    public Result<List<ModelVersionVO>> getVersions(@PathVariable Long modelId) {
        return Result.success(modelVersionService.getVersionsByModelId(modelId));
    }

    @PostMapping
    public Result<?> addVersion(@PathVariable Long modelId, @RequestBody ModelVersionSaveDTO dto) {
        modelVersionService.addVersion(modelId, dto);
        return Result.success("版本创建成功");
    }

    @PutMapping("/{versionId}")
    public Result<?> updateVersion(@PathVariable Long modelId, @PathVariable Long versionId,
                                   @RequestBody ModelVersionSaveDTO dto) {
        dto.setId(versionId);
        modelVersionService.updateVersion(modelId, versionId, dto);
        return Result.success("版本更新成功");
    }

    @DeleteMapping("/{versionId}")
    public Result<?> deleteVersion(@PathVariable Long modelId, @PathVariable Long versionId) {
        modelVersionService.deleteVersion(modelId, versionId);
        return Result.success("版本删除成功");
    }

    @PutMapping("/{versionId}/status")
    public Result<?> toggleStatus(@PathVariable Long modelId, @PathVariable Long versionId,
                                  @RequestParam String status) {
        modelVersionService.toggleStatus(modelId, versionId, status);
        String msg = "RUNNING".equals(status) ? "版本已启动" : "版本已停止";
        return Result.success(msg);
    }
}
