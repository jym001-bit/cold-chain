package com.example.backend.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 跨域过滤器
 * 优先级最高，最先执行
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 允许所有源访问（生产环境应该配置具体域名）
        httpResponse.setHeader("Access-Control-Allow-Origin", "*");

        // 允许的请求方法
        httpResponse.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");

        // 允许的请求头，类型，json等，token携带，Ajax请求
        httpResponse.setHeader("Access-Control-Allow-Headers",
            "Content-Type, Authorization, X-Requested-With");

        // 允许携带凭证
        httpResponse.setHeader("Access-Control-Allow-Credentials", "true");

        // 预检请求的有效期（1小时）
        httpResponse.setHeader("Access-Control-Max-Age", "3600");

        // 处理OPTIONS预检请求，浏览器跨域处理
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }
}
