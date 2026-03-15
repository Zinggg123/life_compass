package com.zing.compass.service;

import com.zing.compass.entity.OrderInfo;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.mapper.OrderMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class OrderService {
    @Autowired
    private CouponService couponService;

    @Resource
    private OrderMapper orderMapper;

    //下单
    public OrderInfo makeOrder(OrderInfo orderInfo) {
        // 1. 验证优惠券
        String couponId = orderInfo.getCouponId();
        String userId = orderInfo.getUserId();
        UserCoupon userCoupon = null;
        if (couponId != null) {
            userCoupon = couponService.validateCoupon(userId, couponId);

            // 2.校验券的金额和实付金额是否匹配
            if(userCoupon.getThredsholdAmount().compareTo(orderInfo.getAmount()) < 0) {
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
            couponService.markCouponAsUsed(userId, couponId);
        }

        return orderInfo;
    }
}
