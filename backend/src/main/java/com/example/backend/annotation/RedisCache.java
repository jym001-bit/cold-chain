package com.example.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Redis缓存注解
 * 用于Service方法上，自动缓存方法返回值
 *
 * 使用示例：
 * @RedisCache(key = "vehicle", ttl = 1800)
 * public Vehicle getVehicleById(Long id) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisCache {

    /**
     * 缓存Key前缀
     */
    String key();

    /**
     * 过期时间（秒）
     */
    long ttl() default 1800;

    /**
     * 是否随机化TTL（防止缓存雪崩）
     */
    boolean randomTtl() default true;
}
