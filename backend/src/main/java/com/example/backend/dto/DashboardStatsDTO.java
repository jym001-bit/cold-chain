package com.example.backend.dto;

import lombok.Data;

/**
 * Dashboard统计数据DTO
 */
@Data
public class DashboardStatsDTO {

    /**
     * 总订单数
     */
    private Long totalOrders;

    /**
     * 总车辆数
     */
    private Long totalVehicles;

    /**
     * 待调度订单数
     */
    private Long pendingOrders;

    /**
     * 空闲车辆数
     */
    private Long idleVehicles;
}
