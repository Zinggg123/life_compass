package com.zing.compass.controller;

import com.zing.compass.service.CouponService;
import com.zing.compass.utils.UserHolder;
import com.zing.compass.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/coupon")
public class CouponController {
    @Autowired
    private CouponService couponService;

    @PostMapping("/grab/{id}")
    public Result grabCoupon(@PathVariable("id") String couponId) {
        try {
            boolean success = couponService.grabCoupon(UserHolder.getUser().getUserId(), couponId);

            if (success) {
                return Result.success("优惠券领券成功");
            } else {
                return Result.failure("抢券失败");
            }
        } catch (RuntimeException e) {
            return Result.failure(e.getMessage());
        } catch (Exception e) {
            return Result.error("An unexpected error occurred");
        }
    }

}
