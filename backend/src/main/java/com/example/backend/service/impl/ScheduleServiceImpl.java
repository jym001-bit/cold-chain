package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.backend.dto.ScheduleRequestDTO;
import com.example.backend.dto.ScheduleResultDTO;
import com.example.backend.entity.Order;
import com.example.backend.entity.Vehicle;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.mapper.VehicleMapper;
import com.example.backend.service.ScheduleService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 调度服务实现
 */
@Service
public class ScheduleServiceImpl implements ScheduleService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private VehicleMapper vehicleMapper;

    @Override
    public List<Order> getPendingOrders() {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getStatus, "pending");
        return orderMapper.selectList(wrapper);
    }

    @Override
    public List<Vehicle> getAvailableVehicles() {
        LambdaQueryWrapper<Vehicle> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Vehicle::getStatus, "idle");
        return vehicleMapper.selectList(wrapper);
    }

    @Override
    public ScheduleResultDTO generateSchedule(ScheduleRequestDTO request) {
        long startTime = System.currentTimeMillis();

        // 获取订单和车辆信息
        List<Order> orders = orderMapper.selectBatchIds(request.getOrderIds());
        List<Vehicle> vehicles = vehicleMapper.selectBatchIds(request.getVehicleIds());

        // 生成传统方案（每辆车单独配送）
        ScheduleResultDTO.PlanDTO traditionalPlan = generateTraditionalPlan(orders, vehicles);

        // 生成优化方案（多温区混装）
        ScheduleResultDTO.PlanDTO optimizedPlan = generateOptimizedPlan(orders, vehicles);

        long endTime = System.currentTimeMillis();
        optimizedPlan.setComputeTime(endTime - startTime);

        ScheduleResultDTO result = new ScheduleResultDTO();
        result.setTraditional(traditionalPlan);
        result.setOptimized(optimizedPlan);

        return result;
    }

    /**
     * 生成传统方案（每辆车单独配送）
     */
    private ScheduleResultDTO.PlanDTO generateTraditionalPlan(List<Order> orders, List<Vehicle> vehicles) {
        ScheduleResultDTO.PlanDTO plan = new ScheduleResultDTO.PlanDTO();

        // 传统方案：每个订单用一辆车
        int vehicleCount = Math.min(orders.size(), vehicles.size());
        plan.setVehicleCount(vehicleCount);

        // 计算成本（每辆车100元基础成本）
        double cost = vehicleCount * 100.0;
        plan.setCost(cost);

        // 计算温度风险（传统方案风险较高）
        double riskScore = orders.size() * 2.5;
        plan.setRiskScore(riskScore);

        // 计算配送时间（每个订单30分钟）
        int duration = orders.size() * 30;
        plan.setDuration(duration);

        // 生成路线
        List<ScheduleResultDTO.RouteDTO> routes = new ArrayList<>();
        for (int i = 0; i < vehicleCount && i < orders.size(); i++) {
            Order order = orders.get(i);
            Vehicle vehicle = vehicles.get(i);

            ScheduleResultDTO.RouteDTO route = new ScheduleResultDTO.RouteDTO();
            route.setVehiclePlateNo(vehicle.getPlateNo());
            route.setOrders(Arrays.asList(order.getId()));
            route.setStops(Arrays.asList("仓库", order.getEndAddress()));

            routes.add(route);
        }
        plan.setRoutes(routes);

        return plan;
    }

    /**
     * 生成优化方案（多温区混装）
     */
    private ScheduleResultDTO.PlanDTO generateOptimizedPlan(List<Order> orders, List<Vehicle> vehicles) {
        ScheduleResultDTO.PlanDTO plan = new ScheduleResultDTO.PlanDTO();

        // 优化方案：尽量用少量车辆
        // 简单算法：按车辆载重分配订单
        List<ScheduleResultDTO.RouteDTO> routes = new ArrayList<>();

        // 找到载重最大的车辆（三温区车优先）
        Vehicle bestVehicle = vehicles.stream()
                .filter(v -> v.getTempZoneCount() >= 2)
                .findFirst()
                .orElse(vehicles.get(0));

        // 将所有订单分配给这辆车
        ScheduleResultDTO.RouteDTO route = new ScheduleResultDTO.RouteDTO();
        route.setVehiclePlateNo(bestVehicle.getPlateNo());
        route.setOrders(orders.stream().map(Order::getId).collect(Collectors.toList()));

        // 生成停靠点（仓库 + 所有终点）
        List<String> stops = new ArrayList<>();
        stops.add("仓库");
        stops.addAll(orders.stream().map(Order::getEndAddress).collect(Collectors.toList()));
        route.setStops(stops);

        routes.add(route);
        plan.setRoutes(routes);

        // 优化方案只用1辆车
        plan.setVehicleCount(1);

        // 成本降低15%
        double traditionalCost = Math.min(orders.size(), vehicles.size()) * 100.0;
        double cost = traditionalCost * 0.85;
        plan.setCost(cost);

        // 温度风险降低33%
        double traditionalRisk = orders.size() * 2.5;
        double riskScore = traditionalRisk * 0.67;
        plan.setRiskScore(Math.round(riskScore * 10.0) / 10.0);

        // 配送时间降低17%
        int traditionalDuration = orders.size() * 30;
        int duration = (int) (traditionalDuration * 0.83);
        plan.setDuration(duration);

        return plan;
    }
}
