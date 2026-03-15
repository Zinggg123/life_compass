package com.zing.compass.controller;

import com.zing.compass.service.RecommendationService;
import com.zing.compass.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class RecommendController {
    private final RecommendationService recommendationService;

    //推荐接口，接收用户ID并返回推荐结果
    @GetMapping("/recommend")
    public Result recommend(@RequestParam String userId) {
        return Result.success(recommendationService.recommend(userId));
    }
}
