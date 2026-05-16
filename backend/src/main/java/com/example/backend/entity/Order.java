package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@TableName("dispatch_order")
public class Order {

    /**
     * 订单ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 货物类型ID
     */
    private Long goodsTypeId;

    /**
     * 货物名称
     */
    private String goodsName;

    /**
     * 重量（kg）
     */
    private BigDecimal weight;

    /**
     * 体积（立方米）
     */
    private BigDecimal volume;

    /**
     * 起点地址
     */
    private String startAddress;

    /**
     * 起点经度
     */
    private BigDecimal startLng;

    /**
     * 起点纬度
     */
    private BigDecimal startLat;

    /**
     * 终点地址
     */
    private String endAddress;

    /**
     * 终点经度
     */
    private BigDecimal endLng;

    /**
     * 终点纬度
     */
    private BigDecimal endLat;

    /**
     * 最早配送时间
     */
    private LocalDateTime earliestTime;

    /**
     * 最晚配送时间
     */
    private LocalDateTime latestTime;

    /**
     * 客户姓名
     */
    private String customerName;

    /**
     * 客户电话
     */
    private String customerPhone;

    /**
     * 状态：pending-待调度，scheduled-已调度，in_transit-执行中，completed-已完成，cancelled-已取消
     */
    private String status;

    /**
     * 创建人ID
     */
    private Long createUserId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
