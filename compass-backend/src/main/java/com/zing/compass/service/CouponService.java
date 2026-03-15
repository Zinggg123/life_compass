package com.zing.compass.service;

import com.zing.compass.entity.Coupon;
import com.zing.compass.entity.OrderInfo;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.mapper.CouponMapper;
import com.zing.compass.mapper.OrderMapper;
import com.zing.compass.mapper.UserCouponMapper;
import com.zing.compass.vo.Result;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CouponService {
    @Resource
    private CouponMapper couponMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private OrderMapper orderMapper;

    //抢券
    public boolean grabCoupon(String userId, String couponId) {
        //1.检查优惠券是否存在
        //2.检查优惠券是否过期
        //3.检查优惠券是否还有库存
        //4.发放优惠券给用户
        return true;
    }

    //检查优惠券是否有效，如果有效返回优惠券信息，否则抛出异常
    public UserCoupon validateCoupon(String userId, String couponId) {
        //1.检查有无优惠券\优惠券是否属于用户
        UserCoupon userCoupon = userCouponMapper.selectByUserIdAndCouponId(userId, couponId);
        if(userCoupon == null) {
            throw new IllegalArgumentException("优惠券不存在");
        }

        //2.检查优惠券是否使用/在使用期限内
        Boolean status = userCoupon.getStatus();
        if(status) {
            throw new IllegalArgumentException("优惠券已使用");
        }

        if(userCoupon.getValidTo().isBefore(LocalDateTime.now())){
            throw new IllegalArgumentException("优惠券已过期");
        } else if(userCoupon.getValidFrom().isAfter(LocalDateTime.now())){
            throw new IllegalArgumentException("优惠券未到生效时间");
        }

        return userCoupon;
    }

    //标记优惠券为已使用
    public boolean markCouponAsUsed(String userId, String userCouponId) {
        return userCouponMapper.updateUserCouponStatus(userCouponId, true);
    }

    //查询用户优惠券
    public List<UserCoupon> getUserCoupons(String userId) {
        return userCouponMapper.selectCouponsByUserId(userId);
    }

    //查询商家优惠券
    public List<Coupon> getBizCoupons(String bizId) {
        return couponMapper.selectCouponsByBizId(bizId);
    }

    //商家发布用户券
    public void addCoupon(String bizId, Coupon coupon){
        //1.检查商家是否存在
        //2.检查优惠券参数是否合法
        //3.插入优惠券数据
        coupon.setCouponId(UUID.randomUUID().toString().replace("-", ""));
        coupon.setBizId(bizId);
        couponMapper.insertCoupon(coupon);
    }
}
