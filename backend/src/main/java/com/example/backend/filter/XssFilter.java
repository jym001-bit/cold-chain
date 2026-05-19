package com.example.backend.filter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * XSS防护过滤器
 * 过滤HTML标签，防止脚本注入
 */
@Component
@Order(1)
public class XssFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 包装请求，过滤参数中的XSS字符
        XssHttpServletRequestWrapper wrappedRequest = new XssHttpServletRequestWrapper(httpRequest);

        chain.doFilter(wrappedRequest, response);
    }

    /**
     * 请求包装类
     */
    private static class XssHttpServletRequestWrapper extends HttpServletRequestWrapper {

        public XssHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return cleanXss(value);
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = cleanXss(values[i]);
            }
            return cleanValues;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            return cleanXss(value);
        }

        /**
         * 清理XSS字符
         */
        private String cleanXss(String value) {
            if (!StringUtils.hasText(value)) {
                return value;
            }

            // 过滤常见的XSS字符
            value = value.replaceAll("<", "&lt;")
                        .replaceAll(">", "&gt;")
                        .replaceAll("\"", "&quot;")
                        .replaceAll("'", "&#x27;")
                        .replaceAll("/", "&#x2F;");

            // 过滤script标签
            value = value.replaceAll("(?i)<script.*?>.*?</script.*?>", "");

            // 过滤iframe标签
            value = value.replaceAll("(?i)<iframe.*?>.*?</iframe.*?>", "");

            // 过滤on事件
            value = value.replaceAll("(?i)on\\w+\\s*=", "");

            return value;
        }
    }
}
