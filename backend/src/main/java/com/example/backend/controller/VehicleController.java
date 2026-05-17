package com.example.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.result.Result;
import com.example.backend.dto.VehicleDTO;
import com.example.backend.dto.VehicleMonitorDTO;
import com.example.backend.entity.Vehicle;
import com.example.backend.service.VehicleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 车辆控制器
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    @Resource
    private VehicleService vehicleService;

    /**
     * 分页查询车辆列表
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @param status 状态筛选（可选）
     * @param keyword 关键词搜索（可选）
     */
    @GetMapping
    public Result<Page<Vehicle>> getVehicleList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        Page<Vehicle> page = vehicleService.getVehicleList(pageNum, pageSize, status, keyword);
        return Result.success(page);
    }

    /**
     * 根据ID查询车辆详情（包含温区配置）
     */
    @GetMapping("/{id}")
    public Result<VehicleDTO> getVehicleById(@PathVariable Long id) {
        VehicleDTO vehicleDTO = vehicleService.getVehicleById(id);
        return Result.success(vehicleDTO);
    }

    /**
     * 新增车辆（包含温区配置）
     */
    @PostMapping
    public Result<String> addVehicle(@RequestBody VehicleDTO vehicleDTO) {
        vehicleService.addVehicle(vehicleDTO);
        return Result.success("新增车辆成功");
    }

    /**
     * 更新车辆（包含温区配置）
     */
    @PutMapping("/{id}")
    public Result<String> updateVehicle(@PathVariable Long id, @RequestBody VehicleDTO vehicleDTO) {
        vehicleDTO.setId(id);
        vehicleService.updateVehicle(vehicleDTO);
        return Result.success("更新车辆成功");
    }

    /**
     * 删除车辆
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return Result.success("删除车辆成功");
    }

    /**
     * 更新车辆状态
     */
    @PutMapping("/{id}/status")
    public Result<String> updateVehicleStatus(@PathVariable Long id, @RequestParam String status) {
        vehicleService.updateVehicleStatus(id, status);
        return Result.success("更新状态成功");
    }

    /**
     * 获取车辆监控数据（用于实时监控大屏）
     */
    @GetMapping("/monitor")
    public Result<List<VehicleMonitorDTO>> getVehicleMonitorData() {
        List<VehicleMonitorDTO> monitorData = vehicleService.getVehicleMonitorData();
        return Result.success(monitorData);
    }
}
