package com.example.backend.filter;

import com.example.backend.util.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * 请求日志过滤器
 * 记录请求参数、响应时间、慢接口告警
 */
@Slf4j
@Component
@Order(2)
public class RequestLogFilter implements Filter {

    private static final long SLOW_API_THRESHOLD = 3000; // 慢接口阈值：3秒

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();

        // 获取请求信息
        String method = httpRequest.getMethod();
        String uri = httpRequest.getRequestURI();
        String ip = IpUtil.getIpAddress(httpRequest);
        String queryString = httpRequest.getQueryString();

        try {
            // 执行请求
            chain.doFilter(request, response);
        } finally {
            // 计算响应时间
            long duration = System.currentTimeMillis() - startTime;

            // 记录请求日志
            String logMessage = String.format("[%s] %s %s | IP: %s | Duration: %dms",
                method, uri, queryString != null ? "?" + queryString : "", ip, duration);

            if (duration > SLOW_API_THRESHOLD) {
                // 慢接口告警
                log.warn("⚠️ SLOW API: {}", logMessage);
                // TODO: 发送到Kafka进行异步处理（后续实现）
            } else {
                log.info(logMessage);
            }
        }
    }
}
