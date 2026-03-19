package com.zing.compass.service;

import com.zing.compass.entity.SimpleBusiness;
import com.zing.compass.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

//推荐服务，提供个性化推荐功能
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserBehaviorService userBehaviorService;
    private final RecommendClient recommendClient;
    private final BizService bizService;

    //推荐接口
    public List<SimpleBusiness> recommend(Integer pageId) {
        if(pageId == null || pageId < 0) {
            pageId = 0;
        }

        String userId = UserHolder.getUser().getUserId();

        String key = "recommend:user:" + userId;
        boolean hasKey = Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));

        if(!hasKey){
            //1. 获取用户最近的行为数据
            List<String> recentBehavior = null;
            if(userId != null && !userId.isEmpty() ){
                recentBehavior = userBehaviorService.getUserRecentBehavior(userId);
            }

            //2.调用Python推荐服务(生成用户向量 → Faiss检索), 只返回推荐店铺ID列表
            List<String> bizIds = recommendClient.recommend(userId, recentBehavior);

            //存入Redis
            if(bizIds != null && !bizIds.isEmpty()){
                stringRedisTemplate.opsForList().rightPushAll(key, bizIds);
                stringRedisTemplate.expire(key, 1, TimeUnit.HOURS);
            }
        }

        //分页获取
        long start = pageId * 10L;
        long end = start + 9;
        List<String> pageBizIds = stringRedisTemplate.opsForList().range(key, start, end);

        if(pageBizIds == null || pageBizIds.isEmpty()){
            return new ArrayList<>();
        }

        System.out.println("推荐的店铺ID列表(Page " + pageId + "): " + pageBizIds.toString());

        //3.根据推荐的店铺ID列表查询店铺详情并返回
        return bizService.getSimpleBusinessesByIds(pageBizIds);
    }
}
