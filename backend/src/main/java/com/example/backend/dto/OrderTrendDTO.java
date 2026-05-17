package com.example.backend.dto;

import lombok.Data;

import java.util.List;

/**
 * 订单趋势DTO
 */
@Data
public class OrderTrendDTO {

    /**
     * 日期列表
     */
    private List<String> dates;

    /**
     * 订单数量列表
     */
    private List<Integer> counts;
}
