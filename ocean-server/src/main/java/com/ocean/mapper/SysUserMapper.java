package com.ocean.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ocean.entity.SysUser;
import com.ocean.dto.UserPageDTO;
import org.apache.ibatis.annotations.Param;

/**
 * 系统用户 Mapper
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 分页查询用户
     */
    IPage<SysUser> selectUserPage(Page<SysUser> page, @Param("dto") UserPageDTO dto);
}
