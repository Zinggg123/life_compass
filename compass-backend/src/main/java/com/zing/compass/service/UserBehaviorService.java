package com.zing.compass.service;

import com.zing.compass.entity.UserBehavior;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//获取用户最近行为记录，提供给推荐系统使用
@Service
@RequiredArgsConstructor
public class UserBehaviorService {
    //Redis
    private final StringRedisTemplate redisTemplate;

    private final CommentService commentService;
    private final OrderService orderService;
    private final CouponService couponService;

    /**
     * 获取用户最近行为记录
     * 先读Redis，如果没有再读MySQL，并将结果缓存到Redis中
     * @param userId 用户ID
     * @return 该用户最近产生交互行为（评论、下单、领券）的商家ID列表
     */
    public List<String> getUserRecentBehavior(String userId) {
        String key = "user:behavior:" + userId;
        Integer limit = 30;

        //1.Redis
        List<String> cachedBehavior = redisTemplate.opsForList().range(key, 0, limit - 1);
        if (!CollectionUtils.isEmpty(cachedBehavior)) {
            return cachedBehavior;
        }

        //2.获取三类行为记录
        List<UserBehavior> commentBizs = commentService.getUserRecentCommentBiz(userId, limit);
        List<UserBehavior> orderBizs = orderService.getUserRecentOrderBiz(userId, limit);
        List<UserBehavior> couponBizs = couponService.getUserRecentCouponBiz(userId, limit);

        //3.融合三个列表内容
        List<UserBehavior> allBehaviors = new ArrayList<>();
        if (!CollectionUtils.isEmpty(commentBizs)) allBehaviors.addAll(commentBizs);
        if (!CollectionUtils.isEmpty(orderBizs)) allBehaviors.addAll(orderBizs);
        if (!CollectionUtils.isEmpty(couponBizs)) allBehaviors.addAll(couponBizs);


        //4.按时间顺序排序（从新到旧），保留前limit个UserBehavior的biz_id作为返回结果
        List<String> result = allBehaviors.stream()
                .sorted(Comparator.comparing(UserBehavior::getTimestamp).reversed())
                .limit(limit)
                .map(UserBehavior::getBizId)
                .distinct()
                .collect(Collectors.toList());

        //5.缓存到Redis
        if (!CollectionUtils.isEmpty(result)) {
            redisTemplate.opsForList().rightPushAll(key, result);
            redisTemplate.expire(key, 24, TimeUnit.HOURS); //设置过期时间
        }

        return result;
    }

}
