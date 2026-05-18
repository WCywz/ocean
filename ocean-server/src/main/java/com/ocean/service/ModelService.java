package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.ModelSaveDTO;
import com.ocean.vo.ModelVO;
import com.ocean.vo.RunningVersionVO;
import java.util.List;

public interface ModelService {
    IPage<ModelVO> getModelPage(Integer pageNum, Integer pageSize, String modelType, String keyword);
    ModelVO getModelById(Long id);
    void addModel(ModelSaveDTO dto);
    void updateModel(ModelSaveDTO dto);
    void deleteModel(Long id);
    List<RunningVersionVO> getRunningVersions();
}
