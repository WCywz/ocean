package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.BusinessException;
import com.ocean.dto.ModelScheduleSaveDTO;
import com.ocean.entity.Model;
import com.ocean.entity.ModelSchedule;
import com.ocean.entity.ModelVersion;
import com.ocean.mapper.ModelMapper;
import com.ocean.mapper.ModelScheduleMapper;
import com.ocean.mapper.ModelVersionMapper;
import com.ocean.service.ModelScheduleService;
import com.ocean.service.SchedulerService;
import com.ocean.util.CronUtil;
import com.ocean.vo.ModelScheduleVO;
import com.ocean.vo.VersionCardVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ModelScheduleServiceImpl implements ModelScheduleService {

    @Autowired private ModelScheduleMapper scheduleMapper;
    @Autowired private ModelVersionMapper versionMapper;
    @Autowired private ModelMapper modelMapper;
    @Autowired private SchedulerService schedulerService;

    @Override
    public List<ModelScheduleVO> getSchedulesByVersionId(Long versionId) {
        List<ModelSchedule> list = scheduleMapper.selectList(
                new LambdaQueryWrapper<ModelSchedule>()
                        .eq(ModelSchedule::getVersionId, versionId)
                        .orderByAsc(ModelSchedule::getScheduleTime));
        return list.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ModelScheduleVO addSchedule(Long versionId, ModelScheduleSaveDTO dto) {
        ModelVersion mv = versionMapper.selectById(versionId);
        if (mv == null) throw new BusinessException("版本不存在");

        ModelSchedule s = new ModelSchedule();
        s.setVersionId(versionId);
        s.setScheduleLabel(dto.getScheduleLabel());
        s.setRepetition(dto.getRepetition());
        s.setDayOfWeek(dto.getDayOfWeek());
        s.setScheduleTime(LocalTime.parse(dto.getScheduleTime(), DateTimeFormatter.ofPattern("HH:mm")));
        if (dto.getScheduleDate() != null && !dto.getScheduleDate().isEmpty()) {
            s.setScheduleDate(LocalDate.parse(dto.getScheduleDate()));
        }
        s.setIsActive(1);
        scheduleMapper.insert(s);

        if ("RUNNING".equals(mv.getStatus())) {
            schedulerService.scheduleOne(s.getId());
        }

        return toVO(s);
    }

    @Override
    public void updateSchedule(Long scheduleId, ModelScheduleSaveDTO dto) {
        ModelSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null) throw new BusinessException("调度不存在");

        s.setScheduleLabel(dto.getScheduleLabel());
        s.setRepetition(dto.getRepetition());
        s.setDayOfWeek(dto.getDayOfWeek());
        s.setScheduleTime(LocalTime.parse(dto.getScheduleTime(), DateTimeFormatter.ofPattern("HH:mm")));
        if (dto.getScheduleDate() != null && !dto.getScheduleDate().isEmpty()) {
            s.setScheduleDate(LocalDate.parse(dto.getScheduleDate()));
        } else {
            s.setScheduleDate(null);
        }
        scheduleMapper.updateById(s);

        if (s.getIsActive() == 1) {
            schedulerService.rescheduleOne(scheduleId);
        }
    }

    @Override
    public void deleteSchedule(Long scheduleId) {
        ModelSchedule s = scheduleMapper.selectById(scheduleId);
        if (s == null) throw new BusinessException("调度不存在");
        scheduleMapper.deleteById(scheduleId);
        schedulerService.unscheduleOne(scheduleId);
    }

    @Override
    public List<ModelScheduleVO> getWeekSchedules(Long modelId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<ModelSchedule> wrapper = new LambdaQueryWrapper<ModelSchedule>()
                .eq(ModelSchedule::getIsActive, 1)
                .orderByAsc(ModelSchedule::getScheduleTime);

        if (modelId != null) {
            List<Long> versionIds = versionMapper.selectList(
                    new LambdaQueryWrapper<ModelVersion>()
                            .eq(ModelVersion::getModelId, modelId)
                            .select(ModelVersion::getId))
                    .stream().map(ModelVersion::getId).toList();
            if (versionIds.isEmpty()) return List.of();
            wrapper.in(ModelSchedule::getVersionId, versionIds);
        }

        List<ModelSchedule> all = scheduleMapper.selectList(wrapper);

        // 过滤 ONCE 调度：仅保留日期在范围内的
        if (startDate != null && endDate != null) {
            all = all.stream().filter(s -> {
                if (!"ONCE".equals(s.getRepetition())) return true;
                if (s.getScheduleDate() == null) return false;
                return !s.getScheduleDate().isBefore(startDate) && !s.getScheduleDate().isAfter(endDate);
            }).collect(Collectors.toList());
        }

        return all.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<VersionCardVO> getAvailableVersionsForSchedule() {
        List<ModelVersion> versions = versionMapper.selectList(null);
        if (versions.isEmpty()) return List.of();

        // Batch load models
        List<Long> modelIds = versions.stream().map(ModelVersion::getModelId).distinct().toList();
        List<Model> models = modelMapper.selectBatchIds(modelIds);
        Map<Long, Model> modelMap = models.stream().collect(Collectors.toMap(Model::getId, m -> m));

        // Batch load all schedules for all versions
        List<Long> versionIds = versions.stream().map(ModelVersion::getId).toList();
        List<ModelSchedule> allSchedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<ModelSchedule>()
                        .in(ModelSchedule::getVersionId, versionIds)
                        .eq(ModelSchedule::getIsActive, 1));
        Map<Long, List<ModelSchedule>> scheduleMap = allSchedules.stream()
                .collect(Collectors.groupingBy(ModelSchedule::getVersionId));

        return versions.stream().map(mv -> {
            VersionCardVO card = new VersionCardVO();
            card.setModelId(mv.getModelId());
            card.setVersionId(mv.getId());
            card.setVersionLabel(mv.getVersionLabel());
            card.setStatus(mv.getStatus());

            Model model = modelMap.get(mv.getModelId());
            if (model != null) {
                card.setModelName(model.getModelName());
                card.setModelType(model.getModelType());
            }

            List<ModelSchedule> schedules = scheduleMap.getOrDefault(mv.getId(), List.of());
            card.setSchedules(schedules.stream().map(s -> {
                VersionCardVO.ScheduleBrief brief = new VersionCardVO.ScheduleBrief();
                brief.setId(s.getId());
                brief.setRepetition(s.getRepetition());
                brief.setDayOfWeek(s.getDayOfWeek());
                brief.setScheduleTime(s.getScheduleTime() != null ? s.getScheduleTime().toString() : null);
                brief.setScheduleDate(s.getScheduleDate() != null ? s.getScheduleDate().toString() : null);
                return brief;
            }).collect(Collectors.toList()));

            return card;
        }).collect(Collectors.toList());
    }

    private ModelScheduleVO toVO(ModelSchedule s) {
        ModelScheduleVO vo = new ModelScheduleVO();
        BeanUtils.copyProperties(s, vo);
        vo.setCronExpression(CronUtil.toCron(s.getRepetition(), s.getDayOfWeek(), s.getScheduleTime(), s.getScheduleDate()));

        ModelVersion mv = versionMapper.selectById(s.getVersionId());
        if (mv != null) {
            vo.setVersionLabel(mv.getVersionLabel());
            Model model = modelMapper.selectById(mv.getModelId());
            if (model != null) {
                vo.setModelName(model.getModelName());
                vo.setModelType(model.getModelType());
            }
        }
        return vo;
    }
}
