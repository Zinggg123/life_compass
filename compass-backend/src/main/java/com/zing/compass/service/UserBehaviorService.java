package com.zing.compass.service;

import com.zing.compass.mapper.UserBehaviorMapper;
import com.zing.compass.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

//获取用户最近行为记录，提供给推荐系统使用
@Service
@RequiredArgsConstructor
public class UserBehaviorService {
    //Redis
    private final StringRedisTemplate redisTemplate;

    //MySQL
    private final UserBehaviorMapper userBehaviorMapper;

    /**
     * 获取用户最近行为记录
     * 先读Redis，如果没有再读MySQL，并将结果缓存到Redis中
     * @param userId
     * @return
     */
    public List<String> getUserRecentBehavior(String userId) {
        String key = "user:behavior:" + userId;

        //1.Redis
        List<String> recentBehavior = redisTemplate.opsForList().range(key, 0, 49); //获取最近“50”条行为记录
        if(recentBehavior != null && !recentBehavior.isEmpty()){
            return recentBehavior;
        }

        //2.MySQL
        recentBehavior = userBehaviorMapper.selectRecentBehavior(userId, 50);

        //3.缓存到Redis
        if(recentBehavior != null && !recentBehavior.isEmpty()){
            redisTemplate.opsForList().rightPushAll(key, recentBehavior);
            redisTemplate.expire(key, 24, TimeUnit.HOURS); //设置过期时间
        }

        //此处可以从数据库或缓存中获取用户的最近行为记录
        return recentBehavior;
    }
}
