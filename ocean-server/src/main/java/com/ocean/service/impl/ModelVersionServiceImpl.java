package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.BusinessException;
import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.entity.Model;
import com.ocean.entity.ModelVersion;
import com.ocean.mapper.ModelMapper;
import com.ocean.mapper.ModelVersionMapper;
import com.ocean.service.ModelVersionService;
import com.ocean.service.SchedulerService;
import com.ocean.vo.ModelVersionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ModelVersionServiceImpl implements ModelVersionService {

    @Autowired private ModelVersionMapper modelVersionMapper;
    @Autowired private ModelMapper modelMapper;
    @Autowired private SchedulerService schedulerService;

    @Override
    public List<ModelVersionVO> getVersionsByModelId(Long modelId) {
        return modelVersionMapper.selectList(
                new LambdaQueryWrapper<ModelVersion>()
                        .eq(ModelVersion::getModelId, modelId)
                        .orderByDesc(ModelVersion::getCreateTime))
                .stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void addVersion(Long modelId, ModelVersionSaveDTO dto) {
        Model model = modelMapper.selectById(modelId);
        if (model == null) throw new BusinessException("模型不存在");
        ModelVersion mv = new ModelVersion();
        mv.setModelId(modelId);
        mv.setVersionLabel(dto.getVersionLabel());
        mv.setParamsConfig(dto.getParamsConfig());
        mv.setCronExpression(dto.getCronExpression());
        mv.setDataSource(dto.getDataSource());
        mv.setDataTimeRange(dto.getDataTimeRange());
        mv.setChangeNote(dto.getChangeNote());
        mv.setStatus("STOPPED");
        modelVersionMapper.insert(mv);
    }

    @Override
    public void updateVersion(Long modelId, Long versionId, ModelVersionSaveDTO dto) {
        ModelVersion mv = modelVersionMapper.selectById(versionId);
        if (mv == null || !mv.getModelId().equals(modelId)) throw new BusinessException("版本不存在");
        mv.setVersionLabel(dto.getVersionLabel());
        mv.setCronExpression(dto.getCronExpression());
        mv.setParamsConfig(dto.getParamsConfig());
        mv.setDataSource(dto.getDataSource());
        mv.setDataTimeRange(dto.getDataTimeRange());
        mv.setChangeNote(dto.getChangeNote());
        modelVersionMapper.updateById(mv);
    }

    @Override
    public void deleteVersion(Long modelId, Long versionId) {
        ModelVersion mv = modelVersionMapper.selectById(versionId);
        if (mv == null || !mv.getModelId().equals(modelId)) throw new BusinessException("版本不存在");
        if ("RUNNING".equals(mv.getStatus())) throw new BusinessException("运行中的版本无法删除");
        modelVersionMapper.deleteById(versionId);
    }

    @Override
    public void toggleStatus(Long modelId, Long versionId, String status) {
        ModelVersion mv = modelVersionMapper.selectById(versionId);
        if (mv == null || !mv.getModelId().equals(modelId)) throw new BusinessException("版本不存在");
        mv.setStatus(status);
        if ("RUNNING".equals(status)) {
            mv.setLastRunTime(LocalDateTime.now());
            modelVersionMapper.updateById(mv);
            schedulerService.schedule(versionId);
        } else {
            modelVersionMapper.updateById(mv);
            schedulerService.unschedule(versionId);
        }
    }

    /**
     * 更新版本属性后，如果版本处于 RUNNING 状态且 cron 发生变化，重新调度。
     */
    public void updateVersionWithReschedule(Long modelId, Long versionId, ModelVersionSaveDTO dto) {
        ModelVersion mv = modelVersionMapper.selectById(versionId);
        if (mv == null || !mv.getModelId().equals(modelId)) throw new BusinessException("版本不存在");
        String oldCron = mv.getCronExpression();
        mv.setVersionLabel(dto.getVersionLabel());
        mv.setCronExpression(dto.getCronExpression());
        mv.setParamsConfig(dto.getParamsConfig());
        mv.setDataSource(dto.getDataSource());
        mv.setDataTimeRange(dto.getDataTimeRange());
        mv.setChangeNote(dto.getChangeNote());
        modelVersionMapper.updateById(mv);
        if ("RUNNING".equals(mv.getStatus()) && !dto.getCronExpression().equals(oldCron)) {
            schedulerService.reschedule(versionId);
        }
    }

    private ModelVersionVO toVO(ModelVersion mv) {
        ModelVersionVO vo = new ModelVersionVO();
        BeanUtils.copyProperties(mv, vo);
        return vo;
    }
}
