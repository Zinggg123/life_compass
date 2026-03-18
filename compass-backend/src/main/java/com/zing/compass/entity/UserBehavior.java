package com.zing.compass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserBehavior {
    private String bizId;
    private Integer behaviorType; //0-评论 1-下单 2-评论（目前只用评论，因为模型训练只用了评论）

    private Long timestamp;
}

//INDEX idx_user_time (user_id, timestamp DESC)
