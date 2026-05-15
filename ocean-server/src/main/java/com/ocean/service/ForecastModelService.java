package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.ModelGroupSaveDTO;
import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.vo.ModelGroupVO;
import com.ocean.vo.ModelVersionVO;
import com.ocean.vo.RunningVersionVO;

import java.util.List;

/**
 * 预报模型服务接口
 */
public interface ForecastModelService {

    /** 分页查询模型组 */
    IPage<ModelGroupVO> getModelPage(Integer pageNum, Integer pageSize, String modelType, String keyword);

    /** 根据ID查询模型组 */
    ModelGroupVO getModelGroupById(Long id);

    /** 新增模型组 */
    void addModelGroup(ModelGroupSaveDTO dto);

    /** 修改模型组 */
    void updateModelGroup(ModelGroupSaveDTO dto);

    /** 删除模型组及其所有版本 */
    void deleteModelGroup(Long id);

    /** 获取所有运行中的版本（概览用） */
    List<RunningVersionVO> getRunningVersions();

    /** 获取模型组下的所有版本 */
    List<ModelVersionVO> getModelVersions(Long groupId);

    /** 新增版本 */
    void addVersion(Long groupId, ModelVersionSaveDTO dto);

    /** 修改版本 */
    void updateVersion(Long groupId, Long versionId, ModelVersionSaveDTO dto);

    /** 删除版本 */
    void deleteVersion(Long groupId, Long versionId);

    /** 启停版本 */
    void toggleVersionStatus(Long groupId, Long versionId, String status);
}
