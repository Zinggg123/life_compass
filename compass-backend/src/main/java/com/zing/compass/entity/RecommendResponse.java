package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//推荐响应实体类，包含推荐结果和相关信息
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecommendResponse {
    private String user_id;
    private List<String> recommendations; //推荐的商户ID列表
    private List<Float> scores; //分数
}
