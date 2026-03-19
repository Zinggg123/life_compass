package com.zing.compass.controller;

import com.zing.compass.entity.Merchant;
import com.zing.compass.service.MerchantService;
import com.zing.compass.vo.LoginRequest;
import com.zing.compass.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//商家（人）和商户（店）是不同的概念
@RestController
@RequestMapping("/merchant")
public class MerchantController {
    @Autowired
    private MerchantService merchantService;

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        try {
            Map<String, Object> data = merchantService.login(req.getMerId(), req.getPassword());
            return Result.success("商家登录成功", data);
        } catch (RuntimeException e) {
            return Result.failure(e.getMessage());
        } catch (Exception e){
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result register(@RequestBody Merchant merchant) {
        try {
            Merchant newMer = merchantService.register(merchant);
            return Result.success("商家注册成功", newMer);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
