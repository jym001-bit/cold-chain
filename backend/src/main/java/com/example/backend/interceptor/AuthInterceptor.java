package com.example.backend.interceptor;

import com.example.backend.common.constant.RedisConstants;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.User;
import com.example.backend.util.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * 认证拦截器
 * 验证JWT Token，从Redis获取用户信息，实现Token续期
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 1. 获取Token
        String token = request.getHeader("Authorization");

        // 去除Bearer前缀
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        if (!StringUtils.hasText(token)) {
            throw new BusinessException("未登录，请先登录");
        }

        // 2. 从Redis获取用户信息（避免每次解析JWT），浪费资源
        String redisKey = RedisConstants.getUserTokenKey(token);
        User user = (User) redisTemplate.opsForValue().get(redisKey);

        if (user == null) {
            throw new BusinessException("登录已过期，请重新登录");
        }

        // 3. 将用户信息存入ThreadLocal
        ThreadLocalUtil.setUser(user);

        // 4. Token续期（滑动窗口机制）
        redisTemplate.expire(redisKey, RedisConstants.TOKEN_EXPIRE_TIME, TimeUnit.SECONDS);

        log.debug("用户认证成功: userId={}, username={}", user.getId(), user.getUsername());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 清理ThreadLocal，防止内存泄漏
        ThreadLocalUtil.clear();
    }
}
