package com.example.backend.service;

import com.example.backend.dto.ScheduleRequestDTO;
import com.example.backend.dto.ScheduleResultDTO;
import com.example.backend.entity.Order;
import com.example.backend.entity.Vehicle;

import java.util.List;

/**
 * 调度服务接口
 */
public interface ScheduleService {

    /**
     * 获取待调度订单列表
     */
    List<Order> getPendingOrders();

    /**
     * 获取可用车辆列表
     */
    List<Vehicle> getAvailableVehicles();

    /**
     * 生成调度方案
     */
    ScheduleResultDTO generateSchedule(ScheduleRequestDTO request);
}
