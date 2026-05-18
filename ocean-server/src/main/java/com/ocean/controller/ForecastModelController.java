package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.ModelGroupSaveDTO;
import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.service.ForecastModelService;
import com.ocean.vo.ModelGroupVO;
import com.ocean.vo.ModelVersionVO;
import com.ocean.vo.RunningVersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预报模型管理控制器
 */
// @RestController -- disabled, replaced by ModelController + ModelVersionController
// @RequestMapping("/api/model")
public class ForecastModelController {

    //@Autowired
    private ForecastModelService forecastModelService;

    // ==================== 模型组 ====================

    /**
     * 分页查询模型组
     */
    @GetMapping("/page")
    public Result<IPage<ModelGroupVO>> getModelPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String keyword) {
        IPage<ModelGroupVO> page = forecastModelService.getModelPage(pageNum, pageSize, modelType, keyword);
        return Result.success(page);
    }

    /**
     * 根据ID查询模型组
     */
    @GetMapping("/{id}")
    public Result<ModelGroupVO> getModelGroupById(@PathVariable Long id) {
        ModelGroupVO vo = forecastModelService.getModelGroupById(id);
        return Result.success(vo);
    }

    /**
     * 新增模型组
     */
    @PostMapping
    public Result<?> addModelGroup(@Validated @RequestBody ModelGroupSaveDTO dto) {
        forecastModelService.addModelGroup(dto);
        return Result.success("模型创建成功");
    }

    /**
     * 修改模型组
     */
    @PutMapping("/{id}")
    public Result<?> updateModelGroup(@PathVariable Long id, @Validated @RequestBody ModelGroupSaveDTO dto) {
        dto.setId(id);
        forecastModelService.updateModelGroup(dto);
        return Result.success("模型更新成功");
    }

    /**
     * 删除模型组及其所有版本
     */
    @DeleteMapping("/{id}")
    public Result<?> deleteModelGroup(@PathVariable Long id) {
        forecastModelService.deleteModelGroup(id);
        return Result.success("模型删除成功");
    }

    // ==================== 运行概览 ====================

    /**
     * 获取所有运行中的版本
     */
    @GetMapping("/running-versions")
    public Result<List<RunningVersionVO>> getRunningVersions() {
        List<RunningVersionVO> list = forecastModelService.getRunningVersions();
        return Result.success(list);
    }

    // ==================== 版本管理 ====================

    /**
     * 获取模型组下的所有版本
     */
    @GetMapping("/{id}/versions")
    public Result<List<ModelVersionVO>> getModelVersions(@PathVariable Long id) {
        List<ModelVersionVO> list = forecastModelService.getModelVersions(id);
        return Result.success(list);
    }

    /**
     * 新增版本
     */
    @PostMapping("/{id}/version")
    public Result<?> addVersion(@PathVariable Long id, @RequestBody ModelVersionSaveDTO dto) {
        forecastModelService.addVersion(id, dto);
        return Result.success("版本创建成功");
    }

    /**
     * 修改版本
     */
    @PutMapping("/{id}/version/{versionId}")
    public Result<?> updateVersion(@PathVariable Long id, @PathVariable Long versionId,
                                    @RequestBody ModelVersionSaveDTO dto) {
        dto.setId(versionId);
        forecastModelService.updateVersion(id, versionId, dto);
        return Result.success("版本更新成功");
    }

    /**
     * 删除版本
     */
    @DeleteMapping("/{id}/version/{versionId}")
    public Result<?> deleteVersion(@PathVariable Long id, @PathVariable Long versionId) {
        forecastModelService.deleteVersion(id, versionId);
        return Result.success("版本删除成功");
    }

    /**
     * 启停版本
     */
    @PutMapping("/{id}/version/{versionId}/status")
    public Result<?> toggleVersionStatus(@PathVariable Long id, @PathVariable Long versionId,
                                          @RequestParam String status) {
        forecastModelService.toggleVersionStatus(id, versionId, status);
        String msg = "RUNNING".equals(status) ? "版本已启动" : "版本已停止";
        return Result.success(msg);
    }
}
