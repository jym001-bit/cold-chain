package com.example.backend.service.impl;

import com.example.backend.common.constant.RedisConstants;
import com.example.backend.entity.GoodsType;
import com.example.backend.mapper.GoodsTypeMapper;
import com.example.backend.service.GoodsTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.backend.common.constant.RedisConstants.GOODS_TYPE_CACHE_EXPIRE_TIME;

/**
 * 货物类型服务实现
 */
@Slf4j
@Service
public class GoodsTypeServiceImpl implements GoodsTypeService {

    @Resource
    private GoodsTypeMapper goodsTypeMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    //使用redis分布式锁，实现防止缓存击穿 热点KEY问题
    @Override
    public List<GoodsType> getAllGoodsTypes() {
        String cacheKey = RedisConstants.GOODS_TYPE_CACHE_KEY;
        String lockKey = RedisConstants.GOODS_TYPE_LOCK_KEY;

        // 1. 先从缓存获取
        List<GoodsType> cachedList = (List<GoodsType>) redisTemplate.opsForValue().get(cacheKey);
        if (cachedList != null) {
            log.debug("从缓存获取货物类型列表，数量: {}", cachedList.size());
            return cachedList;
        }

        // 2. 缓存未命中，尝试获取互斥锁
        //if(locked) 不可以这样写Boolean是包装类 可以是NULL
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
        try {
            if(Boolean.TRUE.equals(locked)) {
                //双重检查,其他请求是否写入缓存
                cachedList = (List<GoodsType>) redisTemplate.opsForValue().get(cacheKey);
                if(cachedList != null) {
                    return cachedList;
                }
                List<GoodsType> list = goodsTypeMapper.selectList(null);
                // 写入缓存（24小时±1小时，防止缓存雪崩）
                // 注意：用 && 而不是 ||，避免空指针异常
                if(list != null && !list.isEmpty()) {
                    redisTemplate.opsForValue().set(
                        cacheKey,
                        list,
                        RedisConstants.getGoodsTypeCacheExpireTime(),
                        TimeUnit.SECONDS
                    );
                    log.debug("货物类型列表已缓存，数量: {}", list.size());
                }
                return list;

            }
            else {
                Thread.sleep(100);
                return getAllGoodsTypes();//递归调用
            }

        } catch (InterruptedException e) {
            // 线程被中断，恢复中断状态并降级查询数据库
            log.error("获取货物类型列表被中断", e);
            Thread.currentThread().interrupt();
            return goodsTypeMapper.selectList(null);
        } catch (Exception e) {
            // 其他异常（如Redis连接失败），降级查询数据库
            log.error("获取货物类型列表失败，降级查询数据库", e);
            return goodsTypeMapper.selectList(null);
        } finally {
            if(Boolean.TRUE.equals(locked)) {
                redisTemplate.delete(lockKey);
            }
        }
    }
}

