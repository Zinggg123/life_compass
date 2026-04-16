package com.zing.compass.service;

import com.zing.compass.dto.UserDTO;
import com.zing.compass.entity.User;
import com.zing.compass.mapper.UserMapper;
import com.zing.compass.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    //登录
    public Map<String, Object> login(String userId, String password) {
        User user = userMapper.selectUserById(userId);
        if(user == null){
            throw new RuntimeException("用户不存在");
        }

        if(password != null && !password.isEmpty()){
            String encryptedPassword = sha256(password);
            if(user.getPassword() != null && !user.getPassword().equals(encryptedPassword)){
                throw new RuntimeException("密码错误");
            }
        }

        // 生成Token
        String token = UUID.randomUUID().toString().replace("-", "");
        
        // Convert User to UserDTO
        UserDTO userDTO = new UserDTO();
        userDTO.setUserId(user.getUserId());
        userDTO.setName(user.getName());
        userDTO.setReviewCount(user.getReviewCount());
        userDTO.setFans(user.getFans());
        userDTO.setElite(user.getElite());
        userDTO.setYelpingSince(user.getYelpingSince());

        // Convert UserDTO to Map<String, String> for Redis Hash
        Map<String, Object> userMap = JSON.parseObject(JSON.toJSONString(userDTO), Map.class);
        Map<String, String> stringUserMap = new HashMap<>();
        userMap.forEach((k, v) -> {
            if(v != null) stringUserMap.put(k, v.toString());
        });

        // 存入Redis
        String key = "user:login:token:" + token;
        redisTemplate.opsForHash().putAll(key, stringUserMap);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);
        
        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("user", userDTO);
        return res;
    }

    //注册
    public User register(User user) {
        User existingUser = userMapper.selectUserById(user.getUserId());
        if(existingUser != null){
            throw new RuntimeException("用户已存在");
        }
        
        if(user.getPassword() == null || user.getPassword().isEmpty()){
            throw new IllegalArgumentException("请输入密码");
        }
        else{
            user.setPassword(sha256(user.getPassword()));
        }

        user.setYelpingSince(LocalDateTime.now());
        userMapper.insertUser(user);
        return user;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // 获取当前登录用户信息（从ThreadLocal读取，不再查库）
    public UserDTO getUserInfo() {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }
        return currentUser;
    }
}
