package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//推荐请求实体类，包含用户ID和其他相关信息
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequest {
    private String user_id;
    private List<String> history_items;
}
