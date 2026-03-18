package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.entity.RecommendRequest;
import com.zing.compass.entity.RecommendResponse;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.List;

//调用Python推荐服务
@Component
public class RecommendClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private final String url = "http://0.0.0.0:8000/recommend";

    public List<String> recommend(String userId, List<String> userBehavior) {
        System.out.println("userId: "+userId);
        System.out.print("userBehavior: ");
        if(userBehavior != null) {
            System.out.println(userBehavior.toString());
        }

        //构建请求参数
        RecommendRequest request = new RecommendRequest();
        request.setUser_id(userId);
        request.setHistory_items(userBehavior);

        // 2. ✅ Fastjson2 手动转 JSON 字符串（最稳）
        String jsonBody = JSON.toJSONString(request);

        // 3. 必须设置 JSON 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 4. 封装 JSON + headers
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        // 5. 发送请求
        RecommendResponse response = restTemplate.postForObject(
                url,
                entity,
                RecommendResponse.class
        );

        //返回推荐结果
        return response.getRecommendations();

    }
}
