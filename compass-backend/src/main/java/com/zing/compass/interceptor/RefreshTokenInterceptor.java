package com.zing.compass.interceptor;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.dto.UserDTO;
import com.zing.compass.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

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
        // 2. 基于TOKEN获取redis中的用户
        String key = "login:token:" + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);
        // 3. 判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        // 4. 将Hash数据转为UserDTO对象
        String json = JSON.toJSONString(userMap);
        UserDTO userDTO = JSON.parseObject(json, UserDTO.class);

        // 5. 存在，保存用户信息到 ThreadLocal
        UserHolder.saveUser(userDTO);
        
        // 6. 刷新token有效期
        stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES);
        
        // 7. 放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 移除用户
        UserHolder.removeUser();
    }
}

