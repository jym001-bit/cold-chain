package com.example.backend.common.constant;

/**
 * Redis常量类
 * 统一管理所有Redis Key的前缀和过期时间
 */
public class RedisConstants {

    // ==================== 用户认证相关 ====================

    /**
     * 用户Token前缀
     * 格式：user:token:{token}
     * 存储内容：User对象
     * 用途：JWT Token验证，避免每次解析JWT
     */
    public static final String USER_TOKEN_PREFIX = "user:token:";
    /**
     * 用户信息前缀
     * 格式：user:info:{userId}
     * 存储内容：User对象
     * 用途：缓存用户基本信息
     */
    public static final String USER_INFO_PREFIX = "user:info:";

    /**
     * Token过期时间（秒）- 7天
     */
    public static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60;

    // ==================== 限流相关 ====================

    /**
     * 限流Key前缀
     * 格式：rate_limit:{ip}:{uri}
     * 存储内容：Hash {tokens: 令牌数, last_time: 上次更新时间}
     * 用途：令牌桶限流算法
     */
    public static final String RATE_LIMIT_PREFIX = "rate_limit:";

    /**
     * 限流Key过期时间（秒）- 1小时
     */
    public static final long RATE_LIMIT_EXPIRE_TIME = 60 * 60;

    // ==================== 缓存相关 ====================

    /**
     * 车辆信息缓存前缀
     * 格式：vehicle:{vehicleId}
     * 存储内容：Vehicle对象
     */
    public static final String VEHICLE_CACHE_PREFIX = "vehicle:";

    /**
     * 车辆缓存过期时间（秒）- 30分钟
     */
    public static final long VEHICLE_CACHE_EXPIRE_TIME = 30 * 60;

    /**
     * 车辆缓存过期时间随机范围（秒）- ±5分钟
     * 用于防止缓存雪崩
     */
    public static final long VEHICLE_CACHE_EXPIRE_RANDOM = 5 * 60;

    public static final long CACHE_NULL_EXPIRE_TIME = 60;

    /**
     * 货物类型缓存Key
     * 格式：goods_type:all
     * 存储内容：List<GoodsType>
     */
    public static final String GOODS_TYPE_CACHE_KEY = "goods_type:all";

    /**
     * 货物类型缓存过期时间（秒）- 24小时
     */
    public static final long GOODS_TYPE_CACHE_EXPIRE_TIME = 24 * 60 * 60;

    /**
     * 货物类型缓存过期时间随机范围（秒）- ±1小时
     * 用于防止缓存雪崩
     */
    public static final long GOODS_TYPE_CACHE_EXPIRE_RANDOM = 60 * 60;

    /**
     * 订单缓存前缀
     * 格式：order:{orderId}
     * 存储内容：Order对象
     */
    public static final String ORDER_CACHE_PREFIX = "order:";

    /**
     * 订单缓存过期时间（秒）- 10分钟
     */
    public static final long ORDER_CACHE_EXPIRE_TIME = 10 * 60;

    // ==================== 分布式锁相关 ====================

    /**
     * 订单号生成锁前缀
     * 格式：lock:order_no:{date}
     */
    public static final String LOCK_ORDER_NO_PREFIX = "lock:order_no:";

    /**
     * 车辆状态更新锁前缀
     * 格式：lock:vehicle:{vehicleId}
     */
    public static final String LOCK_VEHICLE_PREFIX = "lock:vehicle:";

    /**
     * 分布式锁过期时间（秒）- 5秒
     */
    public static final long LOCK_EXPIRE_TIME = 5;

    // ==================== 布隆过滤器相关 ====================

    /**
     * 车辆ID布隆过滤器Key
     */
    public static final String BLOOM_FILTER_VEHICLE_KEY = "bloom:vehicle:ids";

    /**
     * 订单ID布隆过滤器Key
     */
    public static final String BLOOM_FILTER_ORDER_KEY = "bloom:order:ids";

    // ==================== 统计数据缓存 ====================

    /**
     * Dashboard统计数据缓存Key
     */
    public static final String DASHBOARD_STATS_KEY = "dashboard:stats";

    /**
     * Dashboard统计数据过期时间（秒）- 5分钟
     */
    public static final long DASHBOARD_STATS_EXPIRE_TIME = 5 * 60;

    /**
     * Dashboard统计数据过期时间随机范围（秒）- ±30秒
     * 用于防止缓存雪崩
     */
    public static final long DASHBOARD_STATS_EXPIRE_RANDOM = 30;

    /**
     * 车辆监控数据缓存Key
     */
    public static final String VEHICLE_MONITOR_KEY = "vehicle:monitor:all";

    /**
     * 车辆监控数据过期时间（秒）- 30秒
     */
    public static final long VEHICLE_MONITOR_EXPIRE_TIME = 30;

    /**
     * 货物类型分布式锁Key
     * 格式：lock:goods_type
     * 用途：防止缓存击穿
     */
    public static final String GOODS_TYPE_LOCK_KEY = "lock:goods_type";

    // ==================== 工具方法 ====================

    /**
     * 生成用户Token的Redis Key
     */
    public static String getUserTokenKey(String token) {
        return USER_TOKEN_PREFIX + token;
    }

    /**
     * 生成用户信息的Redis Key
     */
    public static String getUserInfoKey(Long userId) {
        return USER_INFO_PREFIX + userId;
    }

    /**
     * 生成限流的Redis Key
     */
    public static String getRateLimitKey(String ip, String uri) {
        return RATE_LIMIT_PREFIX + ip + ":" + uri;
    }

    /**
     * 生成车辆缓存的Redis Key
     */
    public static String getVehicleCacheKey(Long vehicleId) {
        return VEHICLE_CACHE_PREFIX + vehicleId;
    }

    /**
     * 生成订单缓存的Redis Key
     */
    public static String getOrderCacheKey(Long orderId) {
        return ORDER_CACHE_PREFIX + orderId;
    }

    /**
     * 生成订单号生成锁的Redis Key
     */
    public static String getLockOrderNoKey(String date) {
        return LOCK_ORDER_NO_PREFIX + date;
    }

    /**
     * 生成车辆锁的Redis Key
     */
    public static String getLockVehicleKey(Long vehicleId) {
        return LOCK_VEHICLE_PREFIX + vehicleId;
    }

    /**
     * 生成带随机过期时间的缓存时长（防止缓存雪崩）
     *
     * @param baseExpireTime 基础过期时间（秒）
     * @param randomRange 随机范围（秒），实际过期时间 = baseExpireTime ± randomRange
     * @return 随机过期时间（秒）
     */
    public static long getRandomExpireTime(long baseExpireTime, long randomRange) {
        // 生成 [-randomRange, +randomRange] 范围内的随机数
        long randomOffset = (long) (Math.random() * randomRange * 2) - randomRange;
        return baseExpireTime + randomOffset;
    }

    /**
     * 获取车辆缓存的随机过期时间
     * 30分钟 ± 5分钟 = 25-35分钟
     */
    public static long getVehicleCacheExpireTime() {
        return getRandomExpireTime(VEHICLE_CACHE_EXPIRE_TIME, VEHICLE_CACHE_EXPIRE_RANDOM);
    }

    /**
     * 获取货物类型缓存的随机过期时间
     * 24小时 ± 1小时 = 23-25小时
     */
    public static long getGoodsTypeCacheExpireTime() {
        return getRandomExpireTime(GOODS_TYPE_CACHE_EXPIRE_TIME, GOODS_TYPE_CACHE_EXPIRE_RANDOM);
    }

    /**
     * 获取Dashboard统计缓存的随机过期时间
     * 5分钟 ± 30秒 = 4.5-5.5分钟
     */
    public static long getDashboardStatsExpireTime() {
        return getRandomExpireTime(DASHBOARD_STATS_EXPIRE_TIME, DASHBOARD_STATS_EXPIRE_RANDOM);
    }


}
