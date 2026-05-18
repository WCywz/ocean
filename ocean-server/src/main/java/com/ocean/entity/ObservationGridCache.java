package com.ocean.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * 观测网格缓存实体
 */
@Data
@TableName("observation_grid_cache")
public class ObservationGridCache {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Double lat;

    private Double lon;
}
