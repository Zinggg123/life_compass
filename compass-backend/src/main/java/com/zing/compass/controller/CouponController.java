package com.zing.compass.controller;

import com.zing.compass.service.CouponService;
import com.zing.compass.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    @PostMapping("/grab")
    public Result grabCoupon(String userId, String couponId) {
        boolean success = couponService.grabCoupon(userId, couponId);

        if (success) {
            return Result.success("Coupon grabbed successfully");
        } else {
            return Result.failure("Failed to grab coupon");
        }
    }

}
