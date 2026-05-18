package com.ocean.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ocean.common.Result;
import com.ocean.dto.StationSaveDTO;
import com.ocean.service.StationService;
import com.ocean.vo.StationVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/station")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping("/page")
    public Result<IPage<StationVO>> getPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String distance,
            @RequestParam(required = false) String region) {
        return Result.success(stationService.getStationPage(pageNum, pageSize, distance, region));
    }

    @GetMapping("/{id}")
    public Result<StationVO> getById(@PathVariable Long id) {
        return Result.success(stationService.getStationById(id));
    }

    @GetMapping("/by-zone/{zoneId}")
    public Result<List<StationVO>> getByZone(@PathVariable Long zoneId) {
        return Result.success(stationService.getStationsByZoneId(zoneId));
    }

    @PostMapping
    public Result<?> add(@RequestBody StationSaveDTO dto) {
        stationService.addStation(dto);
        return Result.success("站点创建成功");
    }

    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody StationSaveDTO dto) {
        dto.setId(id);
        stationService.updateStation(dto);
        return Result.success("站点更新成功");
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        stationService.deleteStation(id);
        return Result.success("站点删除成功");
    }
}
