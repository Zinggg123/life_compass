package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.dto.UserDTO;
import com.zing.compass.entity.OrderInfo;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.mapper.OrderMapper;
import com.zing.compass.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrderService {
    //Redis
    private final StringRedisTemplate redisTemplate;

    private final CouponService couponService;

    private final OrderMapper orderMapper;

    //下单
    public OrderInfo makeOrder(OrderInfo orderInfo) {
        UserDTO currentUser = UserHolder.getUser();
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        // 1. 验证优惠券
        String couponId = orderInfo.getCouponId();
        String userId = currentUser.getUserId();
        orderInfo.setUserId(userId);
        UserCoupon userCoupon = null;
        if (couponId != null) {
            userCoupon = couponService.validateCoupon(couponId);

            // 2.校验券的金额和实付金额是否匹配
            if(userCoupon.getThresholdAmount().compareTo(orderInfo.getAmount()) > 0) {
                throw new IllegalArgumentException("未达到优惠券使用门槛");
            }
            if (userCoupon.getDiscountAmount().compareTo(orderInfo.getAmount() - orderInfo.getAmountPaid()) != 0) {
                throw new IllegalArgumentException("优惠券金额与订单实付金额不匹配");
            }
        }

        //TODO:用事务控制，保证原子性

        // 3. 创建订单
        String oderId = UUID.randomUUID().toString();
        orderInfo.setOrderId(oderId);
        orderInfo.setOrderTime(LocalDateTime.now());
        orderMapper.insertOrder(orderInfo);

        // 4. 标记优惠券已使用
        if (couponId != null) {
            couponService.markCouponAsUsed(userCoupon.getUserCouponId());
        }

        return orderInfo;
    }

    //获取近期下单记录
    public List<UserBehavior> getUserRecentOrderBiz(String userId, Integer limit) {
        //1.Redis
        String key = "user:order:" + userId;

        //1.Redis
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, limit - 1);

        if (!CollectionUtils.isEmpty(jsonList)) {
            List<UserBehavior> recentOrder = new ArrayList<>();
            for (String json : jsonList) {
                // 手动反序列化
                UserBehavior behavior = JSON.parseObject(json, UserBehavior.class);
                recentOrder.add(behavior);
            }
            return recentOrder;
        }

        //2.MySQL
        List<UserBehavior> recentOrder = orderMapper.selectRecentOrderBiz(userId, limit);

        //3.缓存到Redis
        if (recentOrder != null && !recentOrder.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(key, recentOrder.stream().map(JSON::toJSONString).toList());
            redisTemplate.expire(key, 24, TimeUnit.HOURS); //设置过期时间
        }

        return recentOrder;
    }
}
