package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderInfo {
    private String orderId; //订单ID
    private String userId;  //用户ID
    private String couponId;  //券ID
    private LocalDateTime orderTime; //使用时间
    private Integer amount; //总金额
    private Integer amountPaid; //实付金额
    private String bizId;
}
