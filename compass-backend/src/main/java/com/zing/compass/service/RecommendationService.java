package com.zing.compass.service;

import com.zing.compass.entity.Business;
import com.zing.compass.entity.SimpleBusiness;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

//推荐服务，提供个性化推荐功能
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserBehaviorService userBehaviorService;
    private final RecommendClient recommendClient;
    private final BizService bizService;

    //推荐接口
    public List<SimpleBusiness> recommend(String userId) {
        List<String> recentBehavior = null;

        if(userId != null && !userId.isEmpty() ){
            //1. 获取用户最近的行为数据
            recentBehavior = userBehaviorService.getUserRecentBehavior(userId);
        }

        //2.调用Python推荐服务(生成用户向量 → Faiss检索), 只返回推荐店铺ID列表
        List<String> bizIds = recommendClient.recommend(userId, recentBehavior);

        System.out.println("推荐的店铺ID列表: " + bizIds.toString());

        //3.根据推荐的店铺ID列表查询店铺详情并返回
        return bizService.getSimpleBusinessesByIds(bizIds);
    }
}
