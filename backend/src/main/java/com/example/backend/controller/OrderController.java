package com.example.backend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.result.Result;
import com.example.backend.entity.Order;
import com.example.backend.service.OrderService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 分页查询订单列表
     * @param pageNum 页码（默认1）
     * @param pageSize 每页数量（默认10）
     * @param status 状态筛选（可选）
     * @param keyword 关键词搜索（可选）
     */
    @GetMapping
    public Result<Page<Order>> getOrderList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        Page<Order> page = orderService.getOrderList(pageNum, pageSize, status, keyword);
        return Result.success(page);
    }

    /**
     * 根据ID查询订单详情
     */
    @GetMapping("/{id}")
    public Result<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return Result.success(order);
    }

    /**
     * 新增订单
     */
    @PostMapping
    public Result<String> addOrder(@RequestBody Order order) {
        orderService.addOrder(order);
        return Result.success("新增订单成功");
    }

    /**
     * 更新订单
     */
    @PutMapping("/{id}")
    public Result<String> updateOrder(@PathVariable Long id, @RequestBody Order order) {
        order.setId(id);
        orderService.updateOrder(order);
        return Result.success("更新订单成功");
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return Result.success("删除订单成功");
    }
}
