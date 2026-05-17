package com.example.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货物类型实体类
 */
@Data
@TableName("goods_type")
public class GoodsType {

    /**
     * 货物类型ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 类型名称
     */
    private String typeName;

    /**
     * 最低温度（℃）
     */
    private BigDecimal minTemp;

    /**
     * 最高温度（℃）
     */
    private BigDecimal maxTemp;

    /**
     * 温度敏感度：1-低，2-中，3-高，4-极高
     */
    private Integer sensitivity;

    /**
     * 描述
     */
    private String description;

    /**
     * 关键词（用于智能分类，逗号分隔）
     */
    private String keywords;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
