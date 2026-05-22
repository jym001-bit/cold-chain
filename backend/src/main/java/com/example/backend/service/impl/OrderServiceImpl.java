package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.constant.RedisConstants;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.Order;
import com.example.backend.mapper.OrderMapper;
import com.example.backend.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

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

        // 清理Dashboard缓存（订单数量变化）
        redisTemplate.delete(RedisConstants.DASHBOARD_STATS_KEY);
        log.debug("新增订单成功，已清理Dashboard缓存");
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

        // 清理Dashboard缓存（订单状态可能变化）
        redisTemplate.delete(RedisConstants.DASHBOARD_STATS_KEY);
        log.debug("更新订单成功，已清理Dashboard缓存");
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

        // 清理Dashboard缓存（订单数量变化）
        redisTemplate.delete(RedisConstants.DASHBOARD_STATS_KEY);
        log.debug("删除订单成功，已清理Dashboard缓存");
    }

    /**
     * 生成订单号（并发安全版本）
     * 格式：D + 年月日 + 3位序号
     *
     * 使用Redis原子递增保证并发安全，Redis故障时降级到数据库查询
     */
    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        try {
            // 优先使用Redis原子递增（高性能、并发安全）
            return generateOrderNoFromRedis(date);
        } catch (Exception e) {
            // Redis故障，降级到数据库查询（加分布式锁）
            log.warn("Redis故障，降级到数据库生成订单号", e);
            return generateOrderNoFromDB(date);
        }
    }

    /**
     * 从Redis生成订单号（推荐方案）
     * 使用Redis的INCR命令，保证原子性
     */
    private String generateOrderNoFromRedis(String date) {
        String lockKey = RedisConstants.getLockOrderNoKey(date);

        // Redis原子递增，即使1000个并发请求也不会重复
        Long sequence = redisTemplate.opsForValue().increment(lockKey, 1);

        if (sequence == null) {
            throw new BusinessException("Redis递增失败");
        }

        // 第一次递增时设置过期时间（第二天凌晨就不需要这个计数器了）
        if (sequence == 1) {
            redisTemplate.expire(lockKey, 2, TimeUnit.DAYS);
        }

        log.debug("从Redis生成订单号，日期: {}, 序号: {}", date, sequence);
        return "D" + date + String.format("%03d", sequence);
    }

    /**
     * 从数据库生成订单号（降级方案）
     * 使用Redis分布式锁保证并发安全
     */
    private String generateOrderNoFromDB(String date) {
        String lockKey = "lock:order_no_db:" + date;
        Boolean locked = false;

        try {
            // 尝试获取分布式锁（5秒超时）
            locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);

            if (Boolean.TRUE.equals(locked)) {
                // 获取锁成功，查询数据库
                LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
                wrapper.likeRight(Order::getOrderNo, "D" + date);
                Long count = orderMapper.selectCount(wrapper);

                String sequence = String.format("%03d", count + 1);
                log.debug("从数据库生成订单号（降级），日期: {}, 序号: {}", date, count + 1);
                return "D" + date + sequence;
            } else {
                // 获取锁失败，等待100ms后重试
                Thread.sleep(100);
                return generateOrderNoFromDB(date);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("生成订单号被中断");
        } catch (Exception e) {
            log.error("从数据库生成订单号失败", e);
            throw new BusinessException("生成订单号失败");
        } finally {
            // 释放锁
            if (Boolean.TRUE.equals(locked)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}
