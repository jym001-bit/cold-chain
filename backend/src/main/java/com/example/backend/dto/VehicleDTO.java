package com.example.backend.dto;

import com.example.backend.entity.Vehicle;
import com.example.backend.entity.VehicleTempZone;
import lombok.Data;

import java.util.List;

/**
 * 车辆DTO（包含温区配置）
 */
@Data
public class VehicleDTO extends Vehicle {

    /**
     * 温区配置列表
     */
    private List<VehicleTempZone> tempZones;//多温区
}
