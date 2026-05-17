package com.example.backend.dto;

import lombok.Data;

/**
 * 车辆状态分布DTO
 */
@Data
public class VehicleStatusDTO {

    /**
     * 空闲车辆数
     */
    private Long idle;

    /**
     * 在途车辆数
     */
    private Long busy;

    /**
     * 维修车辆数
     */
    private Long maintenance;
}
