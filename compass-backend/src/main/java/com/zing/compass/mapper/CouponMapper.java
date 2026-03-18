package com.zing.compass.mapper;

import com.zing.compass.entity.Coupon;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouponMapper {
    Coupon selectCouponById(String couponId);

    boolean updateCouponStatus(String couponId, Boolean status);

    List<Coupon> selectCouponsByBizId(String bizId);

    boolean insertCoupon(Coupon coupon);

}
