package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {
    private String merchantId; // 商家ID
    private String name; // 商家名称
    private String email; // 商家邮箱
    private String phone; // 商家联系电话
    private String bizId; //关联的店铺账号（目前仅能关联一家店）
    private LocalDateTime yelpingSince; // 商家注册时间
    private String password; // 商家登录密码
}
