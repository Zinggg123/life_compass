package com.zing.compass.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {
    private String userCouponId; //个人领到的券ID
    private String userId;  //用户ID
    private String couponId;  //券模板ID（关联）
    private Boolean status; //0：未使用, 1:已使用

    private String name;  //冗余字段，方便查询展示
    private Integer discountAmount; //冗余字段，方便查询展示
    private Integer thresholdAmount; //冗余字段，方便查询展示
    private LocalDateTime validFrom; //有效期开始时间戳(冗余字段)
    private LocalDateTime validTo;   //有效期结束时间戳(冗余字段)

    private LocalDateTime getTime; //领取时间
    private LocalDateTime usedTime; //使用时间
    private String order_id; //使用的订单ID（关联）
}
