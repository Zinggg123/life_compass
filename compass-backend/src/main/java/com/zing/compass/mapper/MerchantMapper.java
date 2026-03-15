package com.zing.compass.mapper;

import com.zing.compass.entity.Merchant;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MerchantMapper {
    //根据商家ID查询商家信息
    Merchant selectMerchantById(String merchantId);

    //插入商家信息
    void insertMerchant(Merchant merchant);
}
