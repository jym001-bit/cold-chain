package com.example.backend.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 缓存工具类 — 统一切面，提供缓存穿透/击穿/雪崩三重防护
 *
 * <p>所有 Service 层缓存操作统一通过此工具类，不再手写 Redis 操作：
 * <ul>
 *   <li><b>穿透防护</b>：数据库无结果时缓存空值标记，防止恶意查询打到 DB</li>
 *   <li><b>击穿防护</b>：热点 key 过期时互斥锁 + 双重检查，只让一个线程查库</li>
 *   <li><b>雪崩防护</b>：调用方通过 {@code RedisConstants.getXxxExpireTime()} 传入随机 TTL</li>
 *   <li><b>降级</b>：Redis 异常时自动 Fallback 到数据库</li>
 * </ul>
 *
 * <pre>
 * // 典型用法 — 带互斥锁（热点数据）：
 * public List&lt;GoodsType&gt; getAllGoodsTypes() {
 *     return cacheUtil.getWithMutex(
 *         RedisConstants.GOODS_TYPE_CACHE_KEY,
 *         RedisConstants.getGoodsTypeCacheExpireTime(),
 *         () -> goodsTypeMapper.selectList(null)
 *     );
 * }
 *
 * // 典型用法 — 仅防穿透（普通数据）：
 * public VehicleDTO getVehicleById(Long id) {
 *     return cacheUtil.getWithPassThrough(
 *         RedisConstants.getVehicleCacheKey(id),
 *         RedisConstants.getVehicleCacheExpireTime(),
 *         () -> { ... 查库 &amp; 组装 DTO ... }
 *     );
 * }
 * </pre>
 */
@Slf4j
@Component
public class CacheUtil {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /** 空值缓存过期时间（秒） */
    private static final long NULL_TTL = 60;

    /** 获取锁失败后的重试等待（毫秒） */
    private static final long RETRY_SLEEP_MS = 80;

    /** 互斥锁默认过期时间（秒） */
    private static final long LOCK_TTL = 5;

    // ═══════════════════════════════════════════════════════════════
    //  防穿透（Pass-Through）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 带缓存穿透防护的缓存查询。
     *
     * <p>数据库无数据时缓存 {@link NullValue} 标记 60 秒，后续相同 key 的请求
     * 直接返回 null，不会穿透到数据库。
     */
    public <T> T getWithPassThrough(String key, long expireSeconds, Supplier<T> dbFallback) {
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            //缓存有值
            if (cached != null) {
                if (cached instanceof NullValue) {
                    log.debug("命中空值缓存 -> 穿透已拦截: {}", key);
                    return null;
                }
                log.debug("缓存命中: {}", key);
                return (T) cached;
            }

            T data = dbFallback.get();
            //数据库值是否为NULL对象，value 为NULL会删除KEY的
            if (data != null) {
                redisTemplate.opsForValue().set(key, data, expireSeconds, TimeUnit.SECONDS);
                log.debug("写入缓存: {}", key);
            } else {
                redisTemplate.opsForValue().set(key, new NullValue(), NULL_TTL, TimeUnit.SECONDS);
                log.debug("缓存空值防穿透: {}", key);
            }
            return data;
        } catch (Exception e) {
            log.error("Redis 异常，降级查询数据库: {}", key, e);
            return dbFallback.get();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  防击穿（Mutex Lock + Double Check）
    // ═══════════════════════════════════════════════════════════════

    /**
     * 带缓存击穿 + 穿透防护的缓存查询。
     *
     * <p>热点 key 过期瞬间，多个请求同时发现缓存未命中。互斥锁保证只有一个线程
     * 查库写缓存，其他线程等待后重试从缓存中读取，防止瞬时流量打崩数据库。
     *
     * <p>同时包含穿透防护：数据库无数据时缓存空值。
     */
    public <T> T getWithMutex(String key, long expireSeconds, Supplier<T> dbFallback) {
        // 1. 快路径 — 缓存命中直接返回
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                if (cached instanceof NullValue) {
                    log.debug("命中空值缓存 -> 穿透已拦截: {}", key);
                    return null;
                }
                log.debug("缓存命中: {}", key);
                return (T) cached;
            }
        } catch (Exception e) {
            log.error("Redis 异常，降级查询数据库: {}", key, e);
            return dbFallback.get();
        }

        // 2. 慢路径 — 缓存未命中，争抢互斥锁
        String lockKey = "lock:" + key;
        boolean locked = false;
        //分布式锁，原子性执行
        try {
            locked = Boolean.TRUE.equals(
                    redisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_TTL, TimeUnit.SECONDS));

            if (locked) {
                // === 双重检查 ===
                // 可能已有其他线程写入缓存，再读一次避免重复查库
                Object doubleCheck = redisTemplate.opsForValue().get(key);
                if (doubleCheck != null) {
                    if (doubleCheck instanceof NullValue) {
                        log.debug("双重检查命中空值: {}", key);
                        return null;
                    }
                    log.debug("双重检查命中: {}", key);
                    return (T) doubleCheck;
                }

                // 查库 + 写缓存
                T data = dbFallback.get();
                if (data != null) {
                    redisTemplate.opsForValue().set(key, data, expireSeconds, TimeUnit.SECONDS);
                    log.debug("写入缓存: {}", key);
                } else {
                    redisTemplate.opsForValue().set(key, new NullValue(), NULL_TTL, TimeUnit.SECONDS);
                    log.debug("缓存空值防穿透: {}", key);
                }
                return data;
            } else {
                // 没抢到锁 → 等一会 → 递归（此时缓存大概率已有数据）
                Thread.sleep(RETRY_SLEEP_MS);
                return getWithMutex(key, expireSeconds, dbFallback);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("缓存互斥锁等待被中断: {}", key);
            return dbFallback.get();
        } catch (Exception e) {
            log.error("缓存操作异常，降级查询数据库: {}", key, e);
            return dbFallback.get();
        } finally {
            if (locked) {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception ignored) {
                    // 锁删除失败不影响业务
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  缓存管理
    // ═══════════════════════════════════════════════════════════════

    /** 删除缓存 */
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("缓存已删除: {}", key);
        } catch (Exception e) {
            log.error("删除缓存失败: {}", key, e);
        }
    }

    /** 批量删除 */
    public void delete(String... keys) {
        for (String key : keys) {
            delete(key);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  内部类
    // ═══════════════════════════════════════════════════════════════

    /** 空值标记，用于缓存穿透防护 */
    static class NullValue {
    }
}
