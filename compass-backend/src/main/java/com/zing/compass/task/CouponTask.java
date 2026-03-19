package com.zing.compass.task;

import com.zing.compass.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CouponTask {
    private final CouponService couponService;

    // 每分钟执行一次，预热即将开始的优惠券
    @Scheduled(cron = "0 * * * * ?") //秒 分 时 日 月 周
    public void loadTask() {
        couponService.loadUpcomingCoupons();
    }

    // 每分钟执行一次，清理已过期的优惠券(当前采用Redis自动过期机制)
//    @Scheduled(cron = "0 * * * * ?")
//    public void cleanTask() {
//        couponService.cleanExpiredCoupons();
//    }
}

