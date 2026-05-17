package com.example.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 调度请求DTO
 */
@Data
public class ScheduleRequestDTO {

    /**
     * 订单ID列表
     */
    private List<Long> orderIds;

    /**
     * 车辆ID列表
     */
    private List<Long> vehicleIds;
}
