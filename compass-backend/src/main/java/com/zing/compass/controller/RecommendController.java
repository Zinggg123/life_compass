package com.zing.compass.controller;

import com.zing.compass.service.RecommendationService;
import com.zing.compass.vo.RecommendRequest;
import com.zing.compass.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RecommendController {
    private final RecommendationService recommendationService;

    //推荐接口，接收用户ID并返回推荐结果
    @PostMapping("/recommend")
    public Result recommend(@RequestBody RecommendRequest request) {
        System.out.println(request.toString());
        return Result.success(recommendationService.recommend(request.getPageId()));
    }
}
