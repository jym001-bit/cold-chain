package com.example.backend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.entity.Order;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 分页查询订单列表
     */
    Page<Order> getOrderList(Integer pageNum, Integer pageSize, String status, String keyword);

    /**
     * 根据ID查询订单详情
     */
    Order getOrderById(Long id);

    /**
     * 新增订单
     */
    void addOrder(Order order);

    /**
     * 更新订单
     */
    void updateOrder(Order order);

    /**
     * 删除订单
     */
    void deleteOrder(Long id);
}
