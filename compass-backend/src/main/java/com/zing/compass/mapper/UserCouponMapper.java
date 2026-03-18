package com.zing.compass.mapper;

import com.zing.compass.entity.Coupon;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserCouponMapper {
    UserCoupon selectByUserIdAndCouponId(String userId, String couponId);

    boolean insertUserCoupon(String grabId, String userId, String couponId, LocalDateTime getTime);

    boolean deleteUserCoupon(String userId, String couponId);

    List<UserCoupon> selectCouponsByUserId(String userId);

    boolean updateUserCouponStatus(String userCouponId, Boolean status);

    List<UserBehavior> selectRecentCouponBiz(String userId, Integer limit);

}
