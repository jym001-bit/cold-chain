package com.example.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 车辆监控数据DTO
 */
@Data
public class VehicleMonitorDTO {

    /**
     * 车辆ID
     */
    private Long id;

    /**
     * 车牌号
     */
    private String plateNo;

    /**
     * 状态：idle-空闲，busy-在途，maintenance-维修
     */
    private String status;

    /**
     * 当前温度（℃）
     */
    private BigDecimal currentTemp;

    /**
     * 目标温度（℃）
     */
    private BigDecimal targetTemp;

    /**
     * 当前速度（km/h）
     */
    private Integer speed;

    /**
     * 运行时长（分钟）
     */
    private Integer runtime;

    /**
     * 经度
     */
    private BigDecimal longitude;

    /**
     * 纬度
     */
    private BigDecimal latitude;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 司机电话
     */
    private String driverPhone;
}
