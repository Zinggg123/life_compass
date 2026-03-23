package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.entity.Business;
import com.zing.compass.entity.SimpleBusiness;
import com.zing.compass.mapper.BizMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BizServiceTest {

    @Mock
    private BizMapper bizMapper;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private BizService bizService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void getBusinessesByIds_AllCached() {
        String bizId = "b1";
        List<String> bizIds = Collections.singletonList(bizId);
        Business business = new Business();
        business.setBizId(bizId);
        business.setName("Test Biz");

        when(valueOperations.get("biz:info:" + bizId)).thenReturn(JSON.toJSONString(business));

        List<Business> result = bizService.getBusinessesByIds(bizIds);
        
        assertEquals(1, result.size());
        assertEquals("Test Biz", result.get(0).getName());
        verify(bizMapper, never()).selectBusinessesByIds(anyList());
    }

    @Test
    void getBusinessesByIds_CacheMiss() {
        String bizId = "b1";
        List<String> bizIds = Collections.singletonList(bizId);
        Business business = new Business();
        business.setBizId(bizId);
        business.setName("Test Biz");

        when(valueOperations.get("biz:info:" + bizId)).thenReturn(null);
        when(bizMapper.selectBusinessesByIds(anyList())).thenReturn(Collections.singletonList(business));

        List<Business> result = bizService.getBusinessesByIds(bizIds);

        assertEquals(1, result.size());
        assertEquals("Test Biz", result.get(0).getName());
        verify(valueOperations).set(eq("biz:info:" + bizId), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }

    @Test
    void getBusinessesByIds_EmptyInput() {
        List<Business> result = bizService.getBusinessesByIds(null);
        assertTrue(result.isEmpty());
        
        result = bizService.getBusinessesByIds(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void getSimpleBusinessesByIds_AllCached() {
        String bizId = "b1";
        List<String> bizIds = Collections.singletonList(bizId);
        SimpleBusiness business = new SimpleBusiness();
        business.setBizId(bizId);
        business.setName("Test Biz");

        when(valueOperations.get("biz:simpleinfo:" + bizId)).thenReturn(JSON.toJSONString(business));

        List<SimpleBusiness> result = bizService.getSimpleBusinessesByIds(bizIds);
        
        assertEquals(1, result.size());
        assertEquals("Test Biz", result.get(0).getName());
        verify(bizMapper, never()).selectSimpleBusinessesByIds(anyList());
    }

    @Test
    void getSimpleBusinessesByIds_CacheMiss() {
        String bizId = "b1";
        List<String> bizIds = Collections.singletonList(bizId);
        SimpleBusiness business = new SimpleBusiness();
        business.setBizId(bizId);
        business.setName("Test Biz");

        when(valueOperations.get("biz:simpleinfo:" + bizId)).thenReturn(null);
        when(bizMapper.selectSimpleBusinessesByIds(anyList())).thenReturn(Collections.singletonList(business));

        List<SimpleBusiness> result = bizService.getSimpleBusinessesByIds(bizIds);

        assertEquals(1, result.size());
        assertEquals("Test Biz", result.get(0).getName());
        verify(valueOperations).set(eq("biz:simpleinfo:" + bizId), anyString(), eq(24L), eq(TimeUnit.HOURS));
    }
    
    @Test
    void getSimpleBusinessesByIds_EmptyInput() {
        List<SimpleBusiness> result = bizService.getSimpleBusinessesByIds(null);
        assertTrue(result.isEmpty());
        
        result = bizService.getSimpleBusinessesByIds(Collections.emptyList());
        assertTrue(result.isEmpty());
    }
}


