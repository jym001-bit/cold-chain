package com.example.backend.interceptor;

import com.example.backend.common.exception.BusinessException;
import com.example.backend.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

/**
 * 限流拦截器
 * 基于Redis + Lua脚本实现令牌桶算法
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private DefaultRedisScript<Long> rateLimitScript;

    /**
     * 初始化：加载Lua脚本
     */
    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/rate_limit.lua")));
        rateLimitScript.setResultType(Long.class);
        log.info("令牌桶限流脚本加载成功");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取用户IP地址
        String ip = IpUtil.getIpAddress(request);

        // 获取请求URI，只拿路径，无端口和域名
        String uri = request.getRequestURI();

        // 构建限流Key
        String rateLimitKey = "rate_limit:" + ip + ":" + uri;

        // 根据不同接口设置不同的限流规则
        int capacity;  // 桶容量
        double rate;   // 令牌生成速率（个/秒）

        if (uri.contains("/login") || uri.contains("/register")) {
            // 登录/注册接口：桶容量5，每12秒生成1个令牌（5次/分钟）
            capacity = 5;
            rate = 1.0 / 12.0;
        } else if (uri.contains("/api/orders") || uri.contains("/api/vehicles")) {
            // 写入接口：桶容量20，每3秒生成1个令牌（20次/分钟）
            capacity = 20;
            rate = 1.0 / 3.0;
        } else {
            // 查询接口：桶容量100，每0.6秒生成1个令牌（100次/分钟）
            capacity = 100;
            rate = 1.0 / 0.6;
        }

        // 执行Lua脚本
        long now = System.currentTimeMillis() / 1000; // 当前时间戳（秒）

        Long result = redisTemplate.execute(
            rateLimitScript,
            Collections.singletonList(rateLimitKey),
            String.valueOf(capacity),  // 桶容量
            String.valueOf(rate),      // 令牌生成速率
            "1",                       // 本次请求消耗1个令牌
            String.valueOf(now)        // 当前时间戳
        );

        if (result == null || result == 0) {
            log.warn("限流触发: IP={}, URI={}, Capacity={}, Rate={}/s", ip, uri, capacity, rate);
            throw new BusinessException("请求过于频繁，请稍后再试");
        }

        return true;
    }
}
