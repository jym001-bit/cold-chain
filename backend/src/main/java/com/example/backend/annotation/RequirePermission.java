package com.example.backend.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解
 * 用于Controller方法上，标记需要的权限
 *
 * 使用示例：
 * @RequirePermission("order:add")
 * @RequirePermission("vehicle:delete")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 权限标识
     * 格式：模块:操作
     * 例如：order:add, vehicle:delete, user:update
     */
    String value();
}
