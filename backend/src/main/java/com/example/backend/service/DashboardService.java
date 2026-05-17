package com.example.backend.service;

import com.example.backend.dto.DashboardStatsDTO;
import com.example.backend.dto.OrderTrendDTO;
import com.example.backend.dto.VehicleStatusDTO;

/**
 * Dashboard服务接口
 */
public interface DashboardService {

    /**
     * 获取统计数据
     */
    DashboardStatsDTO getStats();

    /**
     * 获取订单趋势
     */
    OrderTrendDTO getOrderTrend(Integer days);

    /**
     * 获取车辆状态分布
     */
    VehicleStatusDTO getVehicleStatusDistribution();
}
