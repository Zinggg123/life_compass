package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBehavior {
    private Long id; //自增id
    private String userId;
    private String bizId;
    private Integer behaviorType; //0-评论 1-下单 2-评论（目前只用评论，因为模型训练只用了评论）
    private Long timestamp;
}

//INDEX idx_user_time (user_id, timestamp DESC)
