package com.zing.compass.service;

import com.zing.compass.entity.RecommendRequest;
import com.zing.compass.entity.RecommendResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

//调用Python推荐服务
@Component
public class RecommendClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private String url = "http://localhost:5000/recommend";

    public List<String> recommend(String userId, List<String> userBehavior) {
        //构建请求参数
        RecommendRequest request = new RecommendRequest();
        request.setUserId(userId);
        request.setUserBehavior(userBehavior);

        //调用Python推荐服务
        // 1. 生成用户向量（LightGCN/SASRec推理）
        // 2. 去Faiss做向量检索
        // 3. 返回推荐商家列表
        RecommendResponse response = restTemplate.postForObject(url, request, RecommendResponse.class);

        //返回推荐结果
        return response.getRecommendedBusinessIds();

    }
}
