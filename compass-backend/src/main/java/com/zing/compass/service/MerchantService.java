package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.dto.MerchantDTO;
import com.zing.compass.entity.Merchant;
import com.zing.compass.mapper.MerchantMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MerchantService {
    @Resource
    private MerchantMapper merchantMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public Map<String, Object> login(String merId, String password) {
        Merchant merchant = merchantMapper.selectMerchantById(merId);
        if (merchant == null) {
            throw new RuntimeException("商家ID不存在");
        }

        if (password != null && !password.isEmpty()) {
            String encryptedPassword = sha256(password);
            if (merchant.getPassword() != null && !merchant.getPassword().equals(encryptedPassword)) {
                throw new RuntimeException("密码错误");
            }
        }

        // 生成Token
        String token = UUID.randomUUID().toString().replace("-", "");

        // Convert Merchant to MerchantDTO
        MerchantDTO merchantDTO = new MerchantDTO();
        merchantDTO.setMerchantId(merchant.getMerchantId());
        merchantDTO.setName(merchant.getName());
        merchantDTO.setBizId(merchant.getBizId());
        merchantDTO.setYelpingSince(merchant.getYelpingSince());

        // Convert MerchantDTO to Map<String, String> for Redis Hash
        Map<String, Object> merchantMap = JSON.parseObject(JSON.toJSONString(merchantDTO), Map.class);
        Map<String, String> stringMerchantMap = new HashMap<>();
        merchantMap.forEach((k, v) -> {
            if (v != null) stringMerchantMap.put(k, v.toString());
        });

        // 存入Redis
        String key = "merchant:login:token:" + token;
        redisTemplate.opsForHash().putAll(key, stringMerchantMap);
        redisTemplate.expire(key, 30, TimeUnit.MINUTES);

        Map<String, Object> res = new HashMap<>();
        res.put("token", token);
        res.put("merchant", merchantDTO);
        return res;
    }

    public Merchant register(Merchant merchant) {
        // TODO:自动生成账号ID
        Merchant existMer = merchantMapper.selectMerchantById(merchant.getMerchantId());
        if(existMer != null){
            throw new RuntimeException("商户已存在");
        }

        if (merchant.getPassword() == null || merchant.getPassword().isEmpty()) {
            throw new IllegalArgumentException("请输入密码");
        }
        else{
            merchant.setPassword(sha256(merchant.getPassword()));
        }

        merchant.setYelpingSince(LocalDateTime.now());

        merchantMapper.insertMerchant(merchant);
        return merchant;
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
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}

