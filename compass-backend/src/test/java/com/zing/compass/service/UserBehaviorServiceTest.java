package com.zing.compass.service;

import com.zing.compass.entity.UserBehavior;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserBehaviorServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private CommentService commentService;
    @Mock
    private OrderService orderService;
    @Mock
    private CouponService couponService;
    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private UserBehaviorService userBehaviorService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void getUserRecentBehavior_CacheHit() {
        String userId = "u1";
        List<String> cached = Arrays.asList("b1", "b2");
        when(listOperations.range(eq("user:behavior:" + userId), eq(0L), eq(29L)))
                .thenReturn(cached);

        List<String> result = userBehaviorService.getUserRecentBehavior(userId);
        assertEquals(2, result.size());
        assertEquals("b1", result.get(0));
        
        verify(commentService, never()).getUserRecentCommentBiz(anyString(), anyInt());
    }

    @Test
    void getUserRecentBehavior_CacheMiss() {
        String userId = "u1";
        
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);

        UserBehavior ub1 = new UserBehavior("b1", 1, 100L);
        UserBehavior ub2 = new UserBehavior("b2", 2, 200L); // Newer

        when(commentService.getUserRecentCommentBiz(userId, 30)).thenReturn(Collections.singletonList(ub1));
        when(orderService.getUserRecentOrderBiz(userId, 30)).thenReturn(Collections.singletonList(ub2));
        when(couponService.getUserRecentCouponBiz(userId, 30)).thenReturn(Collections.emptyList());

        List<String> result = userBehaviorService.getUserRecentBehavior(userId);

        assertEquals(2, result.size());
        assertEquals("b2", result.get(0)); // Newer first
        assertEquals("b1", result.get(1));

        verify(listOperations).rightPushAll(eq("user:behavior:" + userId), anyList());
        verify(redisTemplate).expire(eq("user:behavior:" + userId), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void getUserRecentBehavior_Empty() {
        String userId = "u1";
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);
        when(commentService.getUserRecentCommentBiz(anyString(), anyInt())).thenReturn(new ArrayList<>());
        when(orderService.getUserRecentOrderBiz(anyString(), anyInt())).thenReturn(new ArrayList<>());
        when(couponService.getUserRecentCouponBiz(anyString(), anyInt())).thenReturn(new ArrayList<>());
        
        List<String> result = userBehaviorService.getUserRecentBehavior(userId);
        assertTrue(result.isEmpty());
        // Should not cache if empty per code logic (verify)
        verify(listOperations, never()).rightPushAll(anyString(), anyList());
    }
}

