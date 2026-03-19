package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.entity.Coupon;
import com.zing.compass.entity.OrderInfo;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.mapper.CouponMapper;
import com.zing.compass.mapper.OrderMapper;
import com.zing.compass.mapper.UserCouponMapper;
import com.zing.compass.utils.RedisIdWorker;
import com.zing.compass.vo.Result;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.transaction.support.TransactionTemplate;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class CouponService {
    //Redis
    private final StringRedisTemplate redisTemplate;
    private final RedisIdWorker redisIdWorker;
    private final RedissonClient redissonClient;
    private final TransactionTemplate transactionTemplate;

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();

    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }
    
    public void addSeckillVoucher(String couponId) {
        // Find DB stock
        Coupon coupon = couponMapper.selectCouponById(couponId);
        if(coupon != null) {
            redisTemplate.opsForValue().set("seckill:stock:" + couponId, String.valueOf(coupon.getAvailableQuantity()));
        }
    }

    //抢券
    public boolean grabCoupon(String userId, String couponId) {
        // 1.生成订单ID
        long orderId = redisIdWorker.nextId("order");

        // 2.执行Lua脚本
        Long result = redisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                couponId, userId, String.valueOf(orderId)
        );

        int r = result.intValue();
        // 3.判断结果
        if (r == 1) {
            throw new RuntimeException("库存不足");
        } else if(r == 2){
            throw new RuntimeException("每人限领一张");
        }

        // 4.返回成功
        return true;
    }

    private class VoucherOrderHandler implements Runnable {
        String queueName = "stream.orders";
        @Override
        public void run() {
            while (true) {
                try {
                    // 1.Create group if not exists (simple try-catch for init)
                    try { //创建消费者组
                        redisTemplate.opsForStream().createGroup(queueName, "g1");
                    } catch (Exception e) {
                        // ignore if exists
                    }
                    
                    // 2.获取消息队列中的订单信息
                    List<MapRecord<String, Object, Object>> list = redisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)), //取一条，空的话等两秒
                            StreamOffset.create(queueName, ReadOffset.lastConsumed()) //上次消费位置
                    );
                    
                    if (list == null || list.isEmpty()) {
                        continue;
                    }
                    
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    
                    // Extract data
                    String userId = (String) value.get("userId");
                    String cId = (String) value.get("voucherId");
                    String orderId = (String) value.get("id");

                    // 3.创建订单
                    handleVoucherOrder(userId, cId, orderId);
                    
                    // 4.ACK
                    redisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId()); //消息id
                    
                } catch (Exception e) {
                    // log.error("Processing error", e);
                    handlePendingList();
                }
            }
        }
        
        private void handlePendingList() {
            while (true) {
                try {
                    List<MapRecord<String, Object, Object>> list = redisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create(queueName, ReadOffset.from("0"))
                    );
                    if (list == null || list.isEmpty()) {
                        break;
                    }
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    String userId = (String) value.get("userId");
                    String cId = (String) value.get("voucherId");
                    String orderId = (String) value.get("id");

                    handleVoucherOrder(userId, cId, orderId);
                    
                    redisTemplate.opsForStream().acknowledge(queueName, "g1", record.getId());
                } catch (Exception e) {
                    break;
                }
            }
        }
    }

    private void handleVoucherOrder(String userId, String couponId, String orderId) {
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        boolean isLock = lock.tryLock();
        if(!isLock){
            //获取锁失败
            //TODO：处理
            return;
        }
        try {
            // 事务提交
            transactionTemplate.execute(status -> {
                // 1. One person one order check (Double check in DB)
                UserCoupon existing = userCouponMapper.selectByUserIdAndCouponId(userId, couponId);
                if (existing != null) {
                    //重复下单
                    //TODO:处理
                    return null;
                }
                
                // 2. Reduce stock (Optimistic Lock)
                int success = couponMapper.updateStockDecrease(couponId);
                if (success < 1) {
                    //库存不足
                    //TODO:处理
                    return null;
                }
                
                // 3. Create Order
                userCouponMapper.insertUserCoupon(orderId, userId, couponId, LocalDateTime.now());
                //TODO:补充UserCoupon冗余字段，方便查询展示
                return null;
            });
        } finally {
            lock.unlock();
        }
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


    //获取近期领取优惠券记录
    public List<UserBehavior> getUserRecentCouponBiz(String userId, Integer limit) {
        //1.Redis
        String key = "user:coupon:" + userId;

        //1.Redis
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, limit - 1);

        if (!CollectionUtils.isEmpty(jsonList)) {
            List<UserBehavior> recentCoupon = new ArrayList<>();
            for (String json : jsonList) {
                // 手动反序列化
                UserBehavior behavior = JSON.parseObject(json, UserBehavior.class);
                recentCoupon.add(behavior);
            }

            return recentCoupon;
        }

        //2.MySQL
        List<UserBehavior> recentCoupon = userCouponMapper.selectRecentCouponBiz(userId, limit);

        //3.缓存到Redis
        if(recentCoupon != null && !recentCoupon.isEmpty()){
            redisTemplate.opsForList().rightPushAll(key, recentCoupon.stream().map(JSON::toJSONString).toArray(String[]::new));
            redisTemplate.expire(key, 24, TimeUnit.HOURS); //设置过期时间
        }

        return recentCoupon;
    }
}
