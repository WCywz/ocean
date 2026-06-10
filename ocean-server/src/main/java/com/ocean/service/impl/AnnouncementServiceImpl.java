package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.common.BusinessException;
import com.ocean.dto.AnnouncementSaveDTO;
import com.ocean.entity.Announcement;
import com.ocean.mapper.AnnouncementMapper;
import com.ocean.service.AnnouncementService;
import com.ocean.vo.AnnouncementVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementServiceImpl implements AnnouncementService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public IPage<AnnouncementVO> getPage(Integer pageNum, Integer pageSize) {
        Page<Announcement> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Announcement::getCreateTime);
        return announcementMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    @Override
    public void add(AnnouncementSaveDTO dto) {
        Announcement announcement = new Announcement();
        BeanUtils.copyProperties(dto, announcement);
        announcementMapper.insert(announcement);
    }

    @Override
    public void update(AnnouncementSaveDTO dto) {
        Announcement announcement = announcementMapper.selectById(dto.getId());
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        BeanUtils.copyProperties(dto, announcement);
        announcementMapper.updateById(announcement);
    }

    @Override
    public void delete(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    private AnnouncementVO toVO(Announcement announcement) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(announcement, vo);
        return vo;
    }
}
