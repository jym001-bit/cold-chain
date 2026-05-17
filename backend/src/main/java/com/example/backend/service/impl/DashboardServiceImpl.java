package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.DashboardStatsDTO;
import com.example.backend.dto.OrderTrendDTO;
import com.example.backend.dto.VehicleStatusDTO;
import com.example.backend.entity.Order;
import com.example.backend.entity.Vehicle;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.VehicleMapper;
import com.example.backend.service.DashboardService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Dashboard服务实现
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private VehicleMapper vehicleMapper;

    @Override
    public DashboardStatsDTO getStats() {
        DashboardStatsDTO stats = new DashboardStatsDTO();

        // 总订单数
        Long totalOrders = orderMapper.selectCount(null);
        stats.setTotalOrders(totalOrders);

        // 总车辆数
        Long totalVehicles = vehicleMapper.selectCount(null);
        stats.setTotalVehicles(totalVehicles);

        // 待调度订单数
        LambdaQueryWrapper<Order> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Order::getStatus, "pending");
        Long pendingOrders = orderMapper.selectCount(orderWrapper);
        stats.setPendingOrders(pendingOrders);

        // 空闲车辆数
        LambdaQueryWrapper<Vehicle> vehicleWrapper = new LambdaQueryWrapper<>();
        vehicleWrapper.eq(Vehicle::getStatus, "idle");
        Long idleVehicles = vehicleMapper.selectCount(vehicleWrapper);
        stats.setIdleVehicles(idleVehicles);

        return stats;
    }

    @Override
    public OrderTrendDTO getOrderTrend(Integer days) {
        OrderTrendDTO trend = new OrderTrendDTO();

        List<String> dates = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
        Random random = new Random();

        // 生成最近N天的数据（模拟数据）
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            dates.add(date.format(formatter));
            // 模拟订单数量（5-20之间）
            counts.add(5 + random.nextInt(16));
        }

        trend.setDates(dates);
        trend.setCounts(counts);

        return trend;
    }

    @Override
    public VehicleStatusDTO getVehicleStatusDistribution() {
        VehicleStatusDTO distribution = new VehicleStatusDTO();

        // 空闲车辆
        LambdaQueryWrapper<Vehicle> idleWrapper = new LambdaQueryWrapper<>();
        idleWrapper.eq(Vehicle::getStatus, "idle");
        Long idle = vehicleMapper.selectCount(idleWrapper);
        distribution.setIdle(idle);

        // 在途车辆
        LambdaQueryWrapper<Vehicle> busyWrapper = new LambdaQueryWrapper<>();
        busyWrapper.eq(Vehicle::getStatus, "busy");
        Long busy = vehicleMapper.selectCount(busyWrapper);
        distribution.setBusy(busy);

        // 维修车辆
        LambdaQueryWrapper<Vehicle> maintenanceWrapper = new LambdaQueryWrapper<>();
        maintenanceWrapper.eq(Vehicle::getStatus, "maintenance");
        Long maintenance = vehicleMapper.selectCount(maintenanceWrapper);
        distribution.setMaintenance(maintenance);

        return distribution;
    }
}
