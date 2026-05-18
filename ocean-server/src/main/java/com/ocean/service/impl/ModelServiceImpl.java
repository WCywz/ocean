package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.common.BusinessException;
import com.ocean.dto.ModelSaveDTO;
import com.ocean.entity.Model;
import com.ocean.entity.ModelVersion;
import com.ocean.mapper.ModelMapper;
import com.ocean.mapper.ModelVersionMapper;
import com.ocean.service.ModelService;
import com.ocean.vo.ModelVO;
import com.ocean.vo.RunningVersionVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ModelServiceImpl implements ModelService {

    @Autowired private ModelMapper modelMapper;
    @Autowired private ModelVersionMapper modelVersionMapper;

    @Override
    public IPage<ModelVO> getModelPage(Integer pageNum, Integer pageSize, String modelType, String keyword) {
        Page<Model> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Model> wrapper = new LambdaQueryWrapper<>();
        if (modelType != null && !modelType.isEmpty()) wrapper.eq(Model::getModelType, modelType);
        if (keyword != null && !keyword.isEmpty()) wrapper.like(Model::getModelName, keyword);
        wrapper.orderByDesc(Model::getCreateTime);
        return modelMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    public ModelVO getModelById(Long id) {
        Model model = modelMapper.selectById(id);
        if (model == null) throw new BusinessException("模型不存在");
        return toVO(model);
    }

    @Override
    public void addModel(ModelSaveDTO dto) {
        Model model = new Model();
        BeanUtils.copyProperties(dto, model);
        modelMapper.insert(model);
    }

    @Override
    public void updateModel(ModelSaveDTO dto) {
        Model model = modelMapper.selectById(dto.getId());
        if (model == null) throw new BusinessException("模型不存在");
        BeanUtils.copyProperties(dto, model);
        modelMapper.updateById(model);
    }

    @Override
    public void deleteModel(Long id) {
        Model model = modelMapper.selectById(id);
        if (model == null) throw new BusinessException("模型不存在");
        LambdaQueryWrapper<ModelVersion> runningWrapper = new LambdaQueryWrapper<>();
        runningWrapper.eq(ModelVersion::getModelId, id).eq(ModelVersion::getStatus, "RUNNING");
        if (modelVersionMapper.selectCount(runningWrapper) > 0)
            throw new BusinessException("模型下存在运行中的版本，请先停止后再删除");
        modelVersionMapper.delete(new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getModelId, id));
        modelMapper.deleteById(id);
    }

    @Override
    public List<RunningVersionVO> getRunningVersions() {
        List<ModelVersion> running = modelVersionMapper.selectList(
                new LambdaQueryWrapper<ModelVersion>().eq(ModelVersion::getStatus, "RUNNING"));
        List<RunningVersionVO> result = new ArrayList<>();
        for (ModelVersion mv : running) {
            RunningVersionVO vo = new RunningVersionVO();
            vo.setVersionId(mv.getId());
            vo.setModelId(mv.getModelId());
            vo.setVersionLabel(mv.getVersionLabel());
            Model model = modelMapper.selectById(mv.getModelId());
            vo.setModelName(model != null ? model.getModelName() : "");
            result.add(vo);
        }
        return result;
    }

    private ModelVO toVO(Model model) {
        ModelVO vo = new ModelVO();
        BeanUtils.copyProperties(model, vo);
        LambdaQueryWrapper<ModelVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelVersion::getModelId, model.getId());
        vo.setVersionCount(modelVersionMapper.selectCount(wrapper));
        wrapper.eq(ModelVersion::getStatus, "RUNNING");
        vo.setRunningCount(modelVersionMapper.selectCount(wrapper));
        return vo;
    }
}
