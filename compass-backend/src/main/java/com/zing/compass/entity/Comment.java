package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    private String commentId; //评论ID
    private String userId;  //用户ID
    private String bizId;   //商家ID
    private Integer score; //评分，1-5
    private String content; //评论内容
    private LocalDateTime timestamp; //评论时间戳
}
