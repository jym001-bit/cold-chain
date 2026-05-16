package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Order;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单服务实现类
 */
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Override
    public Page<Order> getOrderList(Integer pageNum, Integer pageSize, String status, String keyword) {
        // 创建分页对象
        Page<Order> page = new Page<>(pageNum, pageSize);

        // 构建查询条件
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        // 状态筛选
        if (StringUtils.hasText(status)) {
            wrapper.eq(Order::getStatus, status);
        }

        // 关键词搜索（订单号、货物名称、客户名称）
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Order::getOrderNo, keyword)
                    .or()
                    .like(Order::getGoodsName, keyword)
                    .or()
                    .like(Order::getCustomerName, keyword)
            );
        }

        // 按创建时间倒序
        wrapper.orderByDesc(Order::getCreateTime);

        // 执行查询
        return orderMapper.selectPage(page, wrapper);
    }

    @Override
    public Order getOrderById(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    @Override
    public void addOrder(Order order) {
        // 生成订单号（格式：D + 年月日 + 3位序号）
        if (!StringUtils.hasText(order.getOrderNo())) {
            String orderNo = generateOrderNo();
            order.setOrderNo(orderNo);
        }

        // 设置默认状态
        if (!StringUtils.hasText(order.getStatus())) {
            order.setStatus("pending");
        }

        // 插入数据库
        int result = orderMapper.insert(order);
        if (result == 0) {
            throw new BusinessException("新增订单失败");
        }
    }

    @Override
    public void updateOrder(Order order) {
        // 检查订单是否存在
        Order existOrder = orderMapper.selectById(order.getId());
        if (existOrder == null) {
            throw new BusinessException("订单不存在");
        }

        // 检查订单状态（已完成或已取消的订单不能修改）
        if ("completed".equals(existOrder.getStatus()) || "cancelled".equals(existOrder.getStatus())) {
            throw new BusinessException("该订单状态不允许修改");
        }

        // 更新数据库
        int result = orderMapper.updateById(order);
        if (result == 0) {
            throw new BusinessException("更新订单失败");
        }
    }

    @Override
    public void deleteOrder(Long id) {
        // 检查订单是否存在
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        // 检查订单状态（只有待调度的订单可以删除）
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("只有待调度的订单可以删除");
        }

        // 删除数据库
        int result = orderMapper.deleteById(id);
        if (result == 0) {
            throw new BusinessException("删除订单失败");
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        // 格式：D + 年月日 + 3位序号
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // 查询今天已有的订单数量
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.likeRight(Order::getOrderNo, "D" + date);
        Long count = orderMapper.selectCount(wrapper);

        // 生成序号（从001开始）
        String sequence = String.format("%03d", count + 1);

        return "D" + date + sequence;
    }
}
