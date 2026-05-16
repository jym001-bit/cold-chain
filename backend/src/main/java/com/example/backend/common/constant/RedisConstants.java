package com.example.backend.common.constant;

public class RedisConstants {

    /**
     * Token前缀
     */
    public static final String TOKEN_PREFIX = "token:";

    /**
     * 用户信息前缀
     */
    public static final String USER_INFO_PREFIX = "user:info:";

    /**
     * Token过期时间（秒）- 7天
     */
    public static final long TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60;
}
