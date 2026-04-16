package com.zing.compass.controller;

import com.zing.compass.service.RecommendationService;
import com.zing.compass.vo.RecommendRequest;
import com.zing.compass.vo.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequiredArgsConstructor
@Slf4j
public class RecommendController {
    private final RecommendationService recommendationService;

    //推荐接口，接收用户ID并返回推荐结果
    @PostMapping("/recommend")
    public Result recommend(@RequestBody RecommendRequest request) {
        Integer pageId = request == null ? null : request.getPageId();
        log.debug("收到推荐请求 pageId={}", pageId);
        return Result.success(recommendationService.recommend(pageId));
    }
}
