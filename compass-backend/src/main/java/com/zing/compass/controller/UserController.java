package com.zing.compass.controller;

import com.zing.compass.entity.Coupon;
import com.zing.compass.entity.User;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.service.CouponService;
import com.zing.compass.service.UserService;
import com.zing.compass.vo.LoginRequest;
import com.zing.compass.vo.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private CouponService couponService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        try {
            Map<String, Object> data = userService.login(req.getUserId(), req.getPassword());
            return Result.success(data);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        try {
            User newUser = userService.register(user);
            return Result.success(newUser);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/getInfo")
    public Result getUserInfo(String userId) {
        try {
            User user = userService.getUserInfo(userId);
            return Result.success(user);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/coupons")
    public Result getCoupons(String userId) {
        try {
            List<UserCoupon> coupons = couponService.getUserCoupons(userId);
            return Result.success("获取用户优惠券成功", coupons);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
