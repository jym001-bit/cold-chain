package com.example.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 调度结果DTO
 */
@Data
public class ScheduleResultDTO {

    /**
     * 传统方案
     */
    private PlanDTO traditional;

    /**
     * 优化方案
     */
    private PlanDTO optimized;

    /**
     * 方案DTO
     */
    @Data
    public static class PlanDTO {
        /**
         * 车辆数量
         */
        private Integer vehicleCount;

        /**
         * 配送成本
         */
        private Double cost;

        /**
         * 温度风险值
         */
        private Double riskScore;

        /**
         * 配送时间（分钟）
         */
        private Integer duration;

        /**
         * 路线列表
         */
        private List<RouteDTO> routes;

        /**
         * 计算时间（毫秒）
         */
        private Long computeTime;
    }

    /**
     * 路线DTO
     */
    @Data
    public static class RouteDTO {
        /**
         * 车牌号
         */
        private String vehiclePlateNo;

        /**
         * 订单列表
         */
        private List<Long> orders;

        /**
         * 停靠点列表
         */
        private List<String> stops;
    }
}
