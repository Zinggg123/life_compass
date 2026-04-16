package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.dto.MerchantDTO;
import com.zing.compass.dto.UserDTO;
import com.zing.compass.entity.Business;
import com.zing.compass.entity.Coupon;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.mapper.BizMapper;
import com.zing.compass.mapper.CouponMapper;
import com.zing.compass.mapper.UserCouponMapper;
import com.zing.compass.utils.RedisIdWorker;
import com.zing.compass.utils.UserHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private RedisIdWorker redisIdWorker;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private CouponMapper couponMapper;
    @Mock
    private UserCouponMapper userCouponMapper;
    @Mock
    private BizMapper bizMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private ListOperations<String, String> listOperations;
    
    @Mock
    private RLock lock;

    @InjectMocks
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        UserHolder.removeUser();
        UserHolder.removeMerchant();
    }

    @Test
    void addSeckillVoucher() {
        String couponId = "1001";
        Coupon coupon = new Coupon();
        coupon.setCouponId(couponId);
        coupon.setAvailableQuantity(50);
        coupon.setValidFrom(LocalDateTime.now().plusHours(1));
        coupon.setValidTo(LocalDateTime.now().plusHours(2));

        when(couponMapper.selectCouponById(couponId)).thenReturn(coupon);

        couponService.addSeckillVoucher(couponId);

        verify(redisTemplate.opsForValue(), times(3)).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void grabCoupon_Success() {
        String userId = "user1";
        String couponId = "coupon1";
        long orderId = 12345L;
        UserHolder.saveUser(new UserDTO(userId, "name", 0, 0, 0, null));

        when(redisIdWorker.nextId("order")).thenReturn(orderId);
        // Script execution returns 0 for success
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(0L);

        boolean result = couponService.grabCoupon(couponId);
        assertTrue(result);
    }

    @Test
    void grabCoupon_Fail_StockEmpty() {
        String userId = "user1";
        String couponId = "coupon1";
        UserHolder.saveUser(new UserDTO(userId, "name", 0, 0, 0, null));

        when(redisIdWorker.nextId("order")).thenReturn(12345L);
        // Script execution returns 1 for stock empty
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any(), any(), any()))
                .thenReturn(1L);

        Exception exception = assertThrows(RuntimeException.class, () -> couponService.grabCoupon(couponId));
        assertEquals("库存不足", exception.getMessage());
    }

    @Test
    void validateCoupon_Success() {
        String userId = "user1";
        String couponId = "coupon1";
        UserHolder.saveUser(new UserDTO(userId, "name", 0, 0, 0, null));
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(false);
        uc.setValidFrom(LocalDateTime.now().minusDays(1));
        uc.setValidTo(LocalDateTime.now().plusDays(1));

        when(userCouponMapper.selectByUserIdAndCouponId(userId, couponId)).thenReturn(uc);

        UserCoupon result = couponService.validateCoupon(couponId);
        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void validateCoupon_Fail_NotFound() {
        UserHolder.saveUser(new UserDTO("u1", "name", 0, 0, 0, null));
        when(userCouponMapper.selectByUserIdAndCouponId(anyString(), anyString())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> couponService.validateCoupon("c1"));
    }
    
    @Test
    void validateCoupon_Fail_Used() {
        UserHolder.saveUser(new UserDTO("u1", "name", 0, 0, 0, null));
        UserCoupon uc = new UserCoupon();
        uc.setStatus(true);
        when(userCouponMapper.selectByUserIdAndCouponId(anyString(), anyString())).thenReturn(uc);
        assertThrows(IllegalArgumentException.class, () -> couponService.validateCoupon("c1"));
    }

    @Test
    void markCouponAsUsed() {
        String userCouponId = "uc1";
        when(userCouponMapper.updateUserCouponStatus(userCouponId, true)).thenReturn(true);
        boolean result = couponService.markCouponAsUsed(userCouponId);
        assertTrue(result);
    }

    @Test
    void getUserCoupons() {
        String userId = "u1";
        UserHolder.saveUser(new UserDTO(userId, "name", 0, 0, 0, null));
        when(userCouponMapper.selectCouponsByUserId(userId)).thenReturn(new ArrayList<>());
        List<UserCoupon> result = couponService.getUserCoupons();
        assertNotNull(result);
        verify(userCouponMapper).selectCouponsByUserId(userId);
    }

    @Test
    void getBizCoupons() {
        String bizId = "b1";
        UserHolder.saveMerchant(new MerchantDTO("m1", "m", bizId, null));
        when(couponMapper.selectCouponsByBizId(bizId)).thenReturn(new ArrayList<>());
        List<Coupon> result = couponService.getBizCoupons();
        assertNotNull(result);
        verify(couponMapper).selectCouponsByBizId(bizId);
    }

    @Test
    void addCoupon_Success() {
        String bizId = "b1";
        UserHolder.saveMerchant(new MerchantDTO("m1", "m", bizId, null));
        Coupon coupon = new Coupon();
        coupon.setName("Coupon A");
        coupon.setDiscountAmount(100);
        coupon.setThresholdAmount(0);
        coupon.setTotalQuantity(100);
        coupon.setValidFrom(LocalDateTime.now().plusDays(1));
        coupon.setValidTo(LocalDateTime.now().plusDays(5));

        when(bizMapper.selectBusinessById(bizId)).thenReturn(new Business());
        when(couponMapper.insertCoupon(any(Coupon.class))).thenReturn(1);

        assertDoesNotThrow(() -> couponService.addCoupon(coupon));
        
        verify(couponMapper).insertCoupon(any(Coupon.class));
    }
    
    @Test
    void addCoupon_Fail_MissingName() {
        String bizId = "b1";
        UserHolder.saveMerchant(new MerchantDTO("m1", "m", bizId, null));
        Coupon coupon = new Coupon();
        // Missing Name
        
        when(bizMapper.selectBusinessById(bizId)).thenReturn(new Business());
        
        assertThrows(RuntimeException.class, () -> couponService.addCoupon(coupon));
    }

    @Test
    void loadUpcomingCoupons() {
        Coupon c1 = new Coupon();
        c1.setCouponId("c1");
        c1.setValidFrom(LocalDateTime.now().plusMinutes(2));
        c1.setValidTo(LocalDateTime.now().plusHours(1));
        c1.setAvailableQuantity(10);
        
        when(couponMapper.selectFutureValidCoupons(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(c1));
                
        // Simulate redis key miss
        when(redisTemplate.hasKey("seckill:stock:c1")).thenReturn(false);
        
        // Mock coupon lookup for addSeckillVoucher
        when(couponMapper.selectCouponById("c1")).thenReturn(c1);

        couponService.loadUpcomingCoupons();

        verify(redisTemplate.opsForValue(), atLeastOnce()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void cleanExpiredCoupons() {
        when(couponMapper.selectExpiredCouponIds(any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList("c1"));
                
        couponService.cleanExpiredCoupons();
        
        verify(redisTemplate).delete(anyList());
    }

    @Test
    void getUserRecentCouponBiz_CacheHit() {
        String userId = "u1";
        UserBehavior ub = new UserBehavior();
        ub.setBizId("b1");
        String json = JSON.toJSONString(ub);
        
        when(listOperations.range(eq("user:coupon:" + userId), eq(0L), eq(4L)))
                .thenReturn(Collections.singletonList(json));
                
        List<UserBehavior> result = couponService.getUserRecentCouponBiz(userId, 5);
        assertEquals(1, result.size());
        assertEquals("b1", result.get(0).getBizId());
    }

    @Test
    void getUserRecentCouponBiz_CacheMiss() {
        String userId = "u1";
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);
        
        UserBehavior ub = new UserBehavior();
        ub.setBizId("b1");
        
        when(userCouponMapper.selectRecentCouponBiz(userId, 5))
                .thenReturn(Collections.singletonList(ub));
                
        List<UserBehavior> result = couponService.getUserRecentCouponBiz(userId, 5);
        
        assertEquals(1, result.size());
        verify(listOperations).rightPushAll(anyString(), any(String[].class));
    }
}