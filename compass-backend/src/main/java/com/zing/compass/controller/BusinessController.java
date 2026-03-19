package com.zing.compass.controller;

import com.zing.compass.entity.Business;
import com.zing.compass.entity.Coupon;
import com.zing.compass.service.BizService;
import com.zing.compass.service.CouponService;
import com.zing.compass.utils.UserHolder;
import com.zing.compass.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/business")
public class BusinessController {
    @Autowired
    private CouponService couponService;

    @PostMapping("/getCoupons")
    public Result getCoupons(String bizId) {
        try {
            // 这里可以添加获取商家优惠券的逻辑，例如根据商家ID查询优惠券等
            List<Coupon> coupons = couponService.getBizCoupons(bizId);
            return Result.success("获取商家优惠券成功", coupons);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("An unexpected error occurred");
        }
    }

    @PostMapping("/addCoupon")
    public Result addCoupon(@RequestBody Coupon couponInfo) {
        try {
            System.out.println("CouponInfo: "+couponInfo.toString());
            couponService.addCoupon(UserHolder.getMerchant().getBizId(), couponInfo);
            return Result.success("添加商家优惠券成功", null);
        } catch (RuntimeException e) {
            return Result.failure(e.getMessage());
        } catch (Exception e){
            return Result.error("An unexpected error occurred");
        }
    }
}
