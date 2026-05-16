package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆温区配置实体类
 */
@Data
@TableName("vehicle_temp_zone")
public class VehicleTempZone {

    /**
     * 温区配置ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 车辆ID
     */
    private Long vehicleId;

    /**
     * 温区编号：1,2,3
     */
    private Integer zoneNo;

    /**
     * 最低温度（℃）
     */
    private BigDecimal minTemp;

    /**
     * 最高温度（℃）
     */
    private BigDecimal maxTemp;

    /**
     * 容积（立方米）
     */
    private BigDecimal volume;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
