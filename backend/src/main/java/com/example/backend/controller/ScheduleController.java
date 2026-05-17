package com.example.backend.controller;

import com.example.backend.common.result.Result;
import com.example.backend.dto.ScheduleRequestDTO;
import com.example.backend.dto.ScheduleResultDTO;
import com.example.backend.entity.Order;
import com.example.backend.entity.Vehicle;
import com.example.backend.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 调度控制器
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    @Resource
    private ScheduleService scheduleService;

    /**
     * 获取待调度订单列表
     */
    @GetMapping("/pending-orders")
    public Result<List<Order>> getPendingOrders() {
        List<Order> orders = scheduleService.getPendingOrders();
        return Result.success(orders);
    }

    /**
     * 获取可用车辆列表
     */
    @GetMapping("/available-vehicles")
    public Result<List<Vehicle>> getAvailableVehicles() {
        List<Vehicle> vehicles = scheduleService.getAvailableVehicles();
        return Result.success(vehicles);
    }

    /**
     * 生成调度方案
     */
    @PostMapping("/generate")
    public Result<ScheduleResultDTO> generateSchedule(@RequestBody ScheduleRequestDTO request) {
        ScheduleResultDTO result = scheduleService.generateSchedule(request);
        return Result.success(result);
    }
}
