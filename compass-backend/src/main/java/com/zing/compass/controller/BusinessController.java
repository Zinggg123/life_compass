package com.zing.compass.controller;

import com.zing.compass.entity.Coupon;
import com.zing.compass.service.CouponService;
import com.zing.compass.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/business")
@Slf4j
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
            log.warn("查询商家优惠券失败 bizId={}, reason={}", bizId, e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("查询商家优惠券异常 bizId={}", bizId, e);
            return Result.error("An unexpected error occurred");
        }
    }

    @PostMapping("/addCoupon")
    public Result addCoupon(@RequestBody Coupon couponInfo) {
        try {
            log.debug("新增优惠券请求 couponName={}", couponInfo == null ? null : couponInfo.getName());
            couponService.addCoupon(couponInfo);
            return Result.success("添加商家优惠券成功", null);
        } catch (RuntimeException e) {
            log.warn("新增优惠券失败 reason={}", e.getMessage());
            return Result.failure(e.getMessage());
        } catch (Exception e){
            log.error("新增优惠券异常", e);
            return Result.error("An unexpected error occurred");
        }
    }
}
