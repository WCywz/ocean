package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.ModelSaveDTO;
import com.ocean.service.ModelService;
import com.ocean.vo.ModelVO;
import com.ocean.vo.RunningVersionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/model")
public class ModelController {

    @Autowired
    private ModelService modelService;

    @GetMapping("/page")
    public Result<IPage<ModelVO>> getModelPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String modelType,
            @RequestParam(required = false) String keyword) {
        return Result.success(modelService.getModelPage(pageNum, pageSize, modelType, keyword));
    }

    @GetMapping("/{id}")
    public Result<ModelVO> getModelById(@PathVariable Long id) {
        return Result.success(modelService.getModelById(id));
    }

    @PostMapping
    public Result<?> addModel(@Validated @RequestBody ModelSaveDTO dto) {
        modelService.addModel(dto);
        return Result.success("模型创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> updateModel(@PathVariable Long id, @Validated @RequestBody ModelSaveDTO dto) {
        dto.setId(id);
        modelService.updateModel(dto);
        return Result.success("模型更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id);
        return Result.success("模型删除成功");
    }

    @GetMapping("/running-versions")
    public Result<List<RunningVersionVO>> getRunningVersions() {
        return Result.success(modelService.getRunningVersions());
    }
}
