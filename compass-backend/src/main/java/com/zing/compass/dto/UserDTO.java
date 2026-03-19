package com.zing.compass.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private String userId;
    private String name;
    private int reviewCount;
    private int fans; // 粉丝数
    private int elite; // 精英会员等级
    private LocalDateTime yelpingSince; // 加入时间
}

