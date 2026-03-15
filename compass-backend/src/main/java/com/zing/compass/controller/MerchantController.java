package com.zing.compass.controller;

import com.zing.compass.entity.Merchant;
import com.zing.compass.service.MerchantService;
import com.zing.compass.vo.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//商家（人）和商户（店）是不同的概念
@RestController
@RequestMapping("/merchant")
public class MerchantController {
    @Autowired
    private MerchantService merchantService;

    @PostMapping("/login")
    public Result login(String merId) {
        try {
            // 这里可以添加商家登录逻辑，例如验证商家ID等
            Merchant merchant = merchantService.login(merId);
            return Result.success("商家登录成功", merchant);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/register")
    public Result register(Merchant merchant) {
        try {
            Merchant newMer = merchantService.register(merchant);
            return Result.success("商家注册成功", newMer);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
}
