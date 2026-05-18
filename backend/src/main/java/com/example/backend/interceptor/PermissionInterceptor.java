package com.example.backend.interceptor;

import com.example.backend.annotation.RequirePermission;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.User;
import com.example.backend.util.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 权限校验拦截器
 * 基于注解的权限校验
 */
@Slf4j
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 只处理Controller方法
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        // 检查方法上是否有@RequirePermission注解
        RequirePermission annotation = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (annotation == null) {
            // 没有权限注解，直接放行
            return true;
        }

        // 获取当前用户
        User user = ThreadLocalUtil.getUser();
        if (user == null) {
            throw new BusinessException("未登录，请先登录");
        }

        // 获取需要的权限
        String requiredPermission = annotation.value();

        // 检查用户权限
        // TODO: 这里简化处理，实际应该从数据库查询用户的权限列表
        // 目前只做角色判断：admin有所有权限，user只有查询权限
        String role = user.getRole();

        if ("admin".equals(role)) {
            // 管理员拥有所有权限
            return true;
        }

        // 普通用户只能查询
        if (requiredPermission.contains(":view") || requiredPermission.contains(":list")) {
            return true;
        }

        log.warn("⚠️ 权限不足: userId={}, username={}, role={}, requiredPermission={}",
            user.getId(), user.getUsername(), role, requiredPermission);

        throw new BusinessException("权限不足，无法执行该操作");
    }
}
