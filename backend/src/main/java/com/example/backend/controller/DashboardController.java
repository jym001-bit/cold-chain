package com.example.backend.controller;

import com.example.backend.common.result.Result;
import com.example.backend.dto.DashboardStatsDTO;
import com.example.backend.dto.OrderTrendDTO;
import com.example.backend.dto.VehicleStatusDTO;
import com.example.backend.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Dashboard控制器
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    private DashboardService dashboardService;

    /**
     * 获取统计数据
     */
    @GetMapping("/stats")
    public Result<DashboardStatsDTO> getStats() {
        DashboardStatsDTO stats = dashboardService.getStats();
        return Result.success(stats);
    }

    /**
     * 获取订单趋势
     */
    @GetMapping("/order-trend")
    public Result<OrderTrendDTO> getOrderTrend(@RequestParam(defaultValue = "7") Integer days) {
        OrderTrendDTO trend = dashboardService.getOrderTrend(days);
        return Result.success(trend);
    }

    /**
     * 获取车辆状态分布
     */
    @GetMapping("/vehicle-status")
    public Result<VehicleStatusDTO> getVehicleStatusDistribution() {
        VehicleStatusDTO distribution = dashboardService.getVehicleStatusDistribution();
        return Result.success(distribution);
    }
}
