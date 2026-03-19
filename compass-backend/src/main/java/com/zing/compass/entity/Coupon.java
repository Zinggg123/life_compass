package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {
    private String couponId;  //券ID
    private String bizId;   //商家ID
    private String name;  //券名称
    private String description; //券描述
    private Integer discountAmount; //优惠金额
    private Integer thresholdAmount; //使用门槛金额
    private LocalDateTime validFrom; //有效期开始时间戳
    private LocalDateTime validTo;   //有效期结束时间戳

    private Integer totalQuantity; //发放总量
    private Integer availableQuantity; //剩余可用量
    private Boolean status; //0:下架, 1:生效
    private LocalDateTime createTime; //创建时间
    //TODO:到结束时间了自动下架


}
