package com.zing.compass.service;

import com.zing.compass.entity.SimpleBusiness;
import com.zing.compass.utils.UserHolder;
import com.zing.compass.dto.UserDTO;
import org.junit.jupiter.api.AfterEach;
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
class RecommendationServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private UserBehaviorService userBehaviorService;
    @Mock
    private RecommendClient recommendClient;
    @Mock
    private BizService bizService;
    
    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForList()).thenReturn(listOperations);

        UserDTO userDTO = new UserDTO();
        userDTO.setUserId("u1");
        UserHolder.saveUser(userDTO);
    }

    @AfterEach
    void tearDown() {
        UserHolder.removeUser();
    }

    @Test
    void recommend_CacheHit() {
        String userId = "u1";
        String key = "recommend:user:" + userId;
        Integer pageId = 0;

        when(stringRedisTemplate.hasKey(key)).thenReturn(true);
        when(listOperations.range(key, 0, 9)).thenReturn(Arrays.asList("b1", "b2"));

        SimpleBusiness sb1 = new SimpleBusiness();
        sb1.setBizId("b1");
        SimpleBusiness sb2 = new SimpleBusiness();
        sb2.setBizId("b2");
        when(bizService.getSimpleBusinessesByIds(anyList())).thenReturn(Arrays.asList(sb1, sb2));

        List<SimpleBusiness> result = recommendationService.recommend(pageId);
        
        assertEquals(2, result.size());
        verify(recommendClient, never()).recommend(anyString(), anyList());
    }

    @Test
    void recommend_CacheMiss() {
        String userId = "u1";
        String key = "recommend:user:" + userId;
        Integer pageId = 0;

        when(stringRedisTemplate.hasKey(key)).thenReturn(false);
        when(userBehaviorService.getUserRecentBehavior(userId)).thenReturn(Arrays.asList("b3", "b4"));
        
        List<String> mockRecIds = Arrays.asList("b1", "b2");
        when(recommendClient.recommend(eq(userId), anyList())).thenReturn(mockRecIds);
        
        // After caching, it reads from cache
        when(listOperations.range(key, 0, 9)).thenReturn(mockRecIds);
        
        SimpleBusiness sb1 = new SimpleBusiness();
        sb1.setBizId("b1");
        SimpleBusiness sb2 = new SimpleBusiness();
        sb2.setBizId("b2");
        when(bizService.getSimpleBusinessesByIds(mockRecIds)).thenReturn(Arrays.asList(sb1, sb2));

        List<SimpleBusiness> result = recommendationService.recommend(pageId);
        
        assertEquals(2, result.size());
        verify(listOperations).rightPushAll(eq(key), eq(mockRecIds));
        verify(stringRedisTemplate).expire(eq(key), eq(1L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void recommend_PageOffset() {
        String userId = "u1";
        String key = "recommend:user:" + userId;
        Integer pageId = 1; // Start 10, End 19

        when(stringRedisTemplate.hasKey(key)).thenReturn(true);
        when(listOperations.range(key, 10, 19)).thenReturn(Collections.emptyList());

        List<SimpleBusiness> result = recommendationService.recommend(pageId);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void recommend_NoLoginUser_NoNpe() {
        UserHolder.removeUser();
        String key = "recommend:user:empty";

        when(stringRedisTemplate.hasKey(key)).thenReturn(true);
        when(listOperations.range(key, 0, 9)).thenReturn(Collections.emptyList());

        List<SimpleBusiness> result = recommendationService.recommend(0);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}


