package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleBusiness {
    private String bizId;
    private String name;
    private String categories;
    private Double score;  //评分
    private Integer reviewCount; //评论数
}
