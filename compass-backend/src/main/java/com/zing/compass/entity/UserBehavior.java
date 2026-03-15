package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBehavior {
    private String behaviorId; //自增id
    private String userId;
    private String bizId;
    private Integer behaviorType; //0-评论 1-下单（买券不计入？）
    private Long timestamp;
}

//INDEX idx_user_time (user_id, timestamp DESC)
