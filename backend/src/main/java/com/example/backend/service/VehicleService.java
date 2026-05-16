package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.dto.VehicleDTO;
import com.example.backend.entity.Vehicle;

/**
 * 车辆服务接口
 */
public interface VehicleService {

    /**
     * 分页查询车辆列表
     */
    Page<Vehicle> getVehicleList(Integer pageNum, Integer pageSize, String status, String keyword);

    /**
     * 根据ID查询车辆详情（包含温区配置）
     */
    VehicleDTO getVehicleById(Long id);

    /**
     * 新增车辆（包含温区配置）
     */
    void addVehicle(VehicleDTO vehicleDTO);

    /**
     * 更新车辆（包含温区配置）
     */
    void updateVehicle(VehicleDTO vehicleDTO);

    /**
     * 删除车辆
     */
    void deleteVehicle(Long id);

    /**
     * 更新车辆状态
     */
    void updateVehicleStatus(Long id, String status);
}
