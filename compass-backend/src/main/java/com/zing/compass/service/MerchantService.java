package com.zing.compass.service;

import com.zing.compass.entity.Business;
import com.zing.compass.entity.Merchant;
import com.zing.compass.entity.User;
import com.zing.compass.mapper.MerchantMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MerchantService {
    @Resource
    private MerchantMapper merchantMapper;

    public Merchant login(String merId) {
        Merchant merchant = merchantMapper.selectMerchantById(merId);
        if (merchant == null) {
            throw new RuntimeException("商家ID不存在");
        }
        return merchant;
    }

    public Merchant register(Merchant merchant) {
        // TODO:自动生成账号ID
        Merchant existMer = merchantMapper.selectMerchantById(merchant.getMerchantId());
        if(existMer != null){
            throw new RuntimeException("商户已存在");
        }
        merchant.setYelpingSince(LocalDateTime.now());

        merchantMapper.insertMerchant(merchant);
        return merchant;
    }
}
