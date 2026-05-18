package com.ocean.service;

import com.ocean.dto.ModelVersionSaveDTO;
import com.ocean.vo.ModelVersionVO;
import java.util.List;

public interface ModelVersionService {
    List<ModelVersionVO> getVersionsByModelId(Long modelId);
    void addVersion(Long modelId, ModelVersionSaveDTO dto);
    void updateVersion(Long modelId, Long versionId, ModelVersionSaveDTO dto);
    void deleteVersion(Long modelId, Long versionId);
    void toggleStatus(Long modelId, Long versionId, String status);
}
