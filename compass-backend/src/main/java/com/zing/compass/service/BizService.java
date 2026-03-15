package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.entity.Business;
import com.zing.compass.entity.User;
import com.zing.compass.mapper.BizMapper;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

//商家服务类，包含商家相关的业务逻辑
@Service
@RequiredArgsConstructor
public class BizService {
    @Resource
    private BizMapper bizMapper;


    private final StringRedisTemplate redisTemplate;



    /**
     * 根据店铺ID列表查询店铺详情
     * Redis -> MySQL -> 回写
     */
    public List<Business> getBusinessesByIds(List<String> bizIds) {
        if(bizIds == null || bizIds.isEmpty()){
            return Collections.emptyList();
        }

        List<Business> bizs = new ArrayList<>();
        List<String> missedBids = new ArrayList<>();

        //1. Redis
        for(String id:bizIds){
            String key = "biz:info:" + id;
            String str = redisTemplate.opsForValue().get(key);
            if(str != null){
                bizs.add(JSON.parseObject(str, Business.class));
            } else {
                missedBids.add(id);
            }
        }

        if(missedBids.isEmpty()){
            return bizs;
        }

        //2. MySQL
        List<Business> dbBizs = bizMapper.selectBusinessesByIds(missedBids);

        //3. 回写到Redis
        for(Business biz : dbBizs){
            String key = "biz:info:" + biz.getBizId();
            redisTemplate.opsForValue().set(key, JSON.toJSONString(biz), 24, TimeUnit.HOURS);
        }

        bizs.addAll(dbBizs);

        return bizs;
    }
}
