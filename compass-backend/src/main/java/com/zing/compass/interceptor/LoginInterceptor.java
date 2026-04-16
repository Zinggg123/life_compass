package com.zing.compass.interceptor;

import com.zing.compass.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS preflight should not require login context.
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        // 1. 判断是否需要拦截（ThreadLocal中是否有用户或商家）
        if (UserHolder.getUser() == null && UserHolder.getMerchant() == null) {
            log.info("未登录访问：{}", request.getRequestURI());
            // 没有，需要拦截，返回 401
            response.setStatus(401);
            // 拦截
            return false;
        }
        // 有用户或商家，则放行
        return true;
    }
}

