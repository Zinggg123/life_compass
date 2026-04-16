package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.dto.UserDTO;
import com.zing.compass.entity.OrderInfo;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.mapper.OrderMapper;
import com.zing.compass.utils.UserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private CouponService couponService;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        UserHolder.removeUser();
    }

    @Test
    void makeOrder_Success_NoCoupon() {
        UserHolder.saveUser(new UserDTO("u1", "name", 0, 0, 0, null));
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setAmount(100);
        orderInfo.setAmountPaid(100);

        when(orderMapper.insertOrder(any(OrderInfo.class))).thenReturn(1);

        OrderInfo result = orderService.makeOrder(orderInfo);

        assertNotNull(result.getOrderId());
        verify(orderMapper).insertOrder(orderInfo);
        verify(couponService, never()).validateCoupon(anyString());
    }

    @Test
    void makeOrder_Success_WithCoupon() {
        String userId = "u1";
        String couponId = "c1";
        UserHolder.saveUser(new UserDTO(userId, "name", 0, 0, 0, null));
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCouponId(couponId);
        orderInfo.setAmount(100);
        orderInfo.setAmountPaid(90); // 10 discount

        UserCoupon uc = new UserCoupon();
        uc.setUserCouponId("uc1");
        uc.setThresholdAmount(50);
        uc.setDiscountAmount(10);

        when(couponService.validateCoupon(couponId)).thenReturn(uc);
        when(orderMapper.insertOrder(any(OrderInfo.class))).thenReturn(1);

        OrderInfo result = orderService.makeOrder(orderInfo);

        assertNotNull(result.getOrderId());
        verify(couponService).markCouponAsUsed("uc1");
    }

    @Test
    void makeOrder_Fail_ThresholdNotMet() {
        String userId = "u1";
        String couponId = "c1";
        UserHolder.saveUser(new UserDTO(userId, "name", 0, 0, 0, null));
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCouponId(couponId);
        orderInfo.setAmount(40); // Less than 50
        orderInfo.setAmountPaid(30);

        UserCoupon uc = new UserCoupon();
        uc.setThresholdAmount(50);
        uc.setDiscountAmount(10);

        when(couponService.validateCoupon(couponId)).thenReturn(uc);

        assertThrows(IllegalArgumentException.class, () -> orderService.makeOrder(orderInfo));
    }

    @Test
    void makeOrder_Fail_DiscountMismatch() {
        String userId = "u1";
        String couponId = "c1";
        UserHolder.saveUser(new UserDTO(userId, "name", 0, 0, 0, null));
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCouponId(couponId);
        orderInfo.setAmount(100); 
        orderInfo.setAmountPaid(95); // 5 discount, but coupon is 10

        UserCoupon uc = new UserCoupon();
        uc.setThresholdAmount(50);
        uc.setDiscountAmount(10);

        when(couponService.validateCoupon(couponId)).thenReturn(uc);

        assertThrows(IllegalArgumentException.class, () -> orderService.makeOrder(orderInfo));
    }

    @Test
    void getUserRecentOrderBiz_CacheHit() {
        String userId = "u1";
        UserBehavior ub = new UserBehavior();
        ub.setBizId("b1");
        
        when(listOperations.range(eq("user:order:" + userId), eq(0L), eq(4L)))
                .thenReturn(Collections.singletonList(JSON.toJSONString(ub)));

        List<UserBehavior> result = orderService.getUserRecentOrderBiz(userId, 5);
        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).getBizId());
    }

    @Test
    void getUserRecentOrderBiz_CacheMiss() {
        String userId = "u1";
        UserBehavior ub = new UserBehavior();
        ub.setBizId("b1");

        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);
        when(orderMapper.selectRecentOrderBiz(userId, 5)).thenReturn(Collections.singletonList(ub));

        List<UserBehavior> result = orderService.getUserRecentOrderBiz(userId, 5);
        assertEquals(1, result.size());
        
        verify(listOperations).rightPushAll(anyString(), anyList());
    }
}


