package com.zing.compass.interceptor;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.dto.MerchantDTO;
import com.zing.compass.dto.UserDTO;
import com.zing.compass.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取请求头中的token
        String token = request.getHeader("authorization"); // Assuming standard header or "authorization"
        if (token == null || token.isBlank()) {
            return true;
        }

        try {
            // 2. 基于TOKEN获取redis中的用户
            String key = "user:login:token:" + token; //用户key
            Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

            // 如果普通用户不存在，尝试商家
            if (userMap.isEmpty()) {
                key = "merchant:login:token:" + token; //商家key
                userMap = stringRedisTemplate.opsForHash().entries(key);
            }

            // 3. 判断用户是否存在
            if (userMap.isEmpty()) {
                return true;
            }

            // 4. 将Hash数据转为UserDTO或MerchantDTO对象
            String json = JSON.toJSONString(userMap);
            if (key.startsWith("user:login:token:")) {
                UserDTO userDTO = JSON.parseObject(json, UserDTO.class);
                // 5. 存在，保存用户信息到 ThreadLocal
                UserHolder.saveUser(userDTO);
            } else if (key.startsWith("merchant:login:token:")) {
                MerchantDTO merchantDTO = JSON.parseObject(json, MerchantDTO.class);
                UserHolder.saveMerchant(merchantDTO);
            }

            // 6. 刷新token有效期
            stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 这里只记录异常路径，避免正常请求日志噪音
            log.warn("token解析/刷新失败 uri={}, method={}", request.getRequestURI(), request.getMethod(), e);
        }

        // 7. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (ex != null) {
            log.error("请求处理异常 uri={}, method={}", request.getRequestURI(), request.getMethod(), ex);
        }
        // 移除用户和商家
        UserHolder.removeUser();
        UserHolder.removeMerchant();
    }
}

