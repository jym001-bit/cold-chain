package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆实体类
 */
@Data
@TableName("vehicle")
public class Vehicle {

    /**
     * 车辆ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 车牌号
     */
    private String plateNo;

    /**
     * 车型
     */
    private String vehicleType;

    /**
     * 最大载重（kg）
     */
    private BigDecimal maxWeight;

    /**
     * 温区数量：1-单温区，2-双温区，3-三温区
     */
    private Integer tempZoneCount;

    /**
     * 状态：idle-空闲，busy-在途，maintenance-维修
     */
    private String status;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 司机电话
     */
    private String driverPhone;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
