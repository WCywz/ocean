package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.common.BusinessException;
import com.ocean.dto.ModelGroupSaveDTO;
import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.entity.ForecastModel;
import com.ocean.entity.ModelGroup;
import com.ocean.mapper.ForecastModelMapper;
import com.ocean.mapper.ModelGroupMapper;
import com.ocean.service.ForecastModelService;
import com.ocean.vo.ModelGroupVO;
import com.ocean.vo.ModelVersionVO;
import com.ocean.vo.RunningVersionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 预报模型服务实现
 */
@Service
public class ForecastModelServiceImpl implements ForecastModelService {

    @Autowired
    private ModelGroupMapper modelGroupMapper;

    @Autowired
    private ForecastModelMapper forecastModelMapper;

    // ==================== 模型组 CRUD ====================

    @Override
    public IPage<ModelGroupVO> getModelPage(Integer pageNum, Integer pageSize, String modelType, String keyword) {
        Page<ModelGroup> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ModelGroup> wrapper = new LambdaQueryWrapper<>();
        if (modelType != null && !modelType.isEmpty()) {
            wrapper.eq(ModelGroup::getModelType, modelType);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(ModelGroup::getModelName, keyword);
        }
        wrapper.orderByDesc(ModelGroup::getCreateTime);
        IPage<ModelGroup> groupPage = modelGroupMapper.selectPage(page, wrapper);
        return groupPage.convert(this::toGroupVO);
    }

    @Override
    public ModelGroupVO getModelGroupById(Long id) {
        ModelGroup group = modelGroupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("模型组不存在");
        }
        return toGroupVO(group);
    }

    @Override
    public void addModelGroup(ModelGroupSaveDTO dto) {
        ModelGroup group = new ModelGroup();
        BeanUtils.copyProperties(dto, group);
        modelGroupMapper.insert(group);
    }

    @Override
    public void updateModelGroup(ModelGroupSaveDTO dto) {
        ModelGroup group = modelGroupMapper.selectById(dto.getId());
        if (group == null) {
            throw new BusinessException("模型组不存在");
        }
        BeanUtils.copyProperties(dto, group);
        modelGroupMapper.updateById(group);
    }

    @Override
    public void deleteModelGroup(Long id) {
        ModelGroup group = modelGroupMapper.selectById(id);
        if (group == null) {
            throw new BusinessException("模型组不存在");
        }
        // 检查是否有运行中的版本
        LambdaQueryWrapper<ForecastModel> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(ForecastModel::getGroupId, id)
                      .eq(ForecastModel::getStatus, "RUNNING");
        if (forecastModelMapper.selectCount(runningWrapper) > 0) {
            throw new BusinessException("模型组下存在运行中的版本，请先停止后再删除");
        }
        // 删除所有版本
        LambdaQueryWrapper<ForecastModel> versionWrapper = new LambdaQueryWrapper<>();
        versionWrapper.eq(ForecastModel::getGroupId, id);
        forecastModelMapper.delete(versionWrapper);
        // 删除模型组
        modelGroupMapper.deleteById(id);
    }

    private ModelGroupVO toGroupVO(ModelGroup group) {
        ModelGroupVO vo = new ModelGroupVO();
        BeanUtils.copyProperties(group, vo);
        // 统计版本数和运行数
        LambdaQueryWrapper<ForecastModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForecastModel::getGroupId, group.getId());
        long totalVersions = forecastModelMapper.selectCount(wrapper);
        wrapper.eq(ForecastModel::getStatus, "RUNNING");
        long runningVersions = forecastModelMapper.selectCount(wrapper);
        vo.setVersionCount(totalVersions);
        vo.setRunningCount(runningVersions);
        return vo;
    }

    // ==================== 版本 CRUD ====================

    @Override
    public List<RunningVersionVO> getRunningVersions() {
        LambdaQueryWrapper<ForecastModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForecastModel::getStatus, "RUNNING");
        List<ForecastModel> runningModels = forecastModelMapper.selectList(wrapper);

        List<RunningVersionVO> result = new ArrayList<>();
        for (ForecastModel model : runningModels) {
            RunningVersionVO vo = new RunningVersionVO();
            vo.setVersionId(model.getId());
            vo.setModelId(model.getGroupId());
            vo.setVersionLabel(model.getVersionLabel());

            // 查找模型组名称
            if (model.getGroupId() != null) {
                ModelGroup group = modelGroupMapper.selectById(model.getGroupId());
                vo.setModelName(group != null ? group.getModelName() : model.getModelName());
            } else {
                vo.setModelName(model.getModelName());
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<ModelVersionVO> getModelVersions(Long groupId) {
        LambdaQueryWrapper<ForecastModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForecastModel::getGroupId, groupId)
               .orderByDesc(ForecastModel::getCreateTime);
        List<ForecastModel> models = forecastModelMapper.selectList(wrapper);
        List<ModelVersionVO> result = new ArrayList<>();
        for (ForecastModel model : models) {
            result.add(toVersionVO(model));
        }
        return result;
    }

    @Override
    public void addVersion(Long groupId, ModelVersionSaveDTO dto) {
        ModelGroup group = modelGroupMapper.selectById(groupId);
        if (group == null) {
            throw new BusinessException("模型组不存在");
        }
        ForecastModel model = new ForecastModel();
        model.setGroupId(groupId);
        model.setModelName(group.getModelName());
        model.setModelType(group.getModelType());
        model.setVersionLabel(dto.getVersionLabel());
        model.setCronExpression(dto.getCronExpression());
        model.setParamsConfig(dto.getParamsConfig());
        model.setDataSource(dto.getDataSource());
        model.setDataTimeRange(dto.getDataTimeRange());
        model.setChangeNote(dto.getChangeNote());
        model.setStatus("STOPPED");
        forecastModelMapper.insert(model);
    }

    @Override
    public void updateVersion(Long groupId, Long versionId, ModelVersionSaveDTO dto) {
        ForecastModel model = forecastModelMapper.selectById(versionId);
        if (model == null || !model.getGroupId().equals(groupId)) {
            throw new BusinessException("版本不存在");
        }
        model.setVersionLabel(dto.getVersionLabel());
        model.setCronExpression(dto.getCronExpression());
        model.setParamsConfig(dto.getParamsConfig());
        model.setDataSource(dto.getDataSource());
        model.setDataTimeRange(dto.getDataTimeRange());
        model.setChangeNote(dto.getChangeNote());
        forecastModelMapper.updateById(model);
    }

    @Override
    public void deleteVersion(Long groupId, Long versionId) {
        ForecastModel model = forecastModelMapper.selectById(versionId);
        if (model == null || !model.getGroupId().equals(groupId)) {
            throw new BusinessException("版本不存在");
        }
        if ("RUNNING".equals(model.getStatus())) {
            throw new BusinessException("运行中的版本无法删除，请先停止版本");
        }
        forecastModelMapper.deleteById(versionId);
    }

    @Override
    public void toggleVersionStatus(Long groupId, Long versionId, String status) {
        ForecastModel model = forecastModelMapper.selectById(versionId);
        if (model == null || !model.getGroupId().equals(groupId)) {
            throw new BusinessException("版本不存在");
        }
        model.setStatus(status);
        if ("RUNNING".equals(status)) {
            model.setLastRunTime(LocalDateTime.now());
        }
        forecastModelMapper.updateById(model);
    }

    private ModelVersionVO toVersionVO(ForecastModel model) {
        ModelVersionVO vo = new ModelVersionVO();
        BeanUtils.copyProperties(model, vo);
        return vo;
    }
}
