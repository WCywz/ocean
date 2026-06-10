package com.ocean.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.dto.AnnouncementSaveDTO;
import com.ocean.vo.AnnouncementVO;

public interface AnnouncementService {
    IPage<AnnouncementVO> getPage(Integer pageNum, Integer pageSize);
    void add(AnnouncementSaveDTO dto);
    void update(AnnouncementSaveDTO dto);
    void delete(Long id);
}
