package org.markeb.hotswap.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.markeb.hotswap.config.HotSwapProperties;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 热更新接口认证拦截器
 */
public class HotSwapAuthInterceptor implements HandlerInterceptor {

    private final HotSwapProperties properties;

    public HotSwapAuthInterceptor(HotSwapProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HotSwapProperties.HttpConfig httpConfig = properties.getHttp();

        // 如果未启用认证，直接放行
        if (!httpConfig.isAuthEnabled()) {
            return true;
        }

        // 获取请求中的 token
        String token = request.getHeader("X-HotSwap-Token");
        if (token == null) {
            token = request.getParameter("token");
        }

        // 校验 token
        String expectedToken = httpConfig.getAuthToken();
        if (expectedToken != null && expectedToken.equals(token)) {
            return true;
        }

        // 认证失败
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Invalid or missing token\"}");
        return false;
    }

}

