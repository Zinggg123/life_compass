package com.zing.compass.service;

import com.alibaba.fastjson2.JSON;
import com.zing.compass.entity.Coupon;
import com.zing.compass.entity.OrderInfo;
import com.zing.compass.entity.UserBehavior;
import com.zing.compass.entity.UserCoupon;
import com.zing.compass.mapper.BizMapper;
import com.zing.compass.mapper.CouponMapper;
import com.zing.compass.mapper.OrderMapper;
import com.zing.compass.mapper.UserCouponMapper;
import com.zing.compass.utils.RedisIdWorker;
import com.zing.compass.vo.Result;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
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
    private final BizMapper bizMapper;

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
            long nowSec = System.currentTimeMillis() / 1000;
            long startSec = coupon.getValidFrom().toEpochSecond(ZoneOffset.of("+8")); //转成时间戳，东八区
            long endSec = coupon.getValidTo().toEpochSecond(ZoneOffset.of("+8"));
            long ttlSeconds = endSec - nowSec + 60;

            redisTemplate.opsForValue().set("seckill:stock:" + couponId, String.valueOf(coupon.getAvailableQuantity()), ttlSeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set("seckill:startTime:" + couponId, String.valueOf(startSec), ttlSeconds, TimeUnit.SECONDS);
            redisTemplate.opsForValue().set("seckill:endTime:" + couponId, String.valueOf(endSec), ttlSeconds, TimeUnit.SECONDS);
        }
    }

    //抢券
    public boolean grabCoupon(String userId, String couponId) {
        // 1.生成订单ID
        long orderId = redisIdWorker.nextId("order");
        
        // Current timestamp for Lua
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.of("+8"));

        // 2.执行Lua脚本
        Long result = redisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                couponId, userId, String.valueOf(orderId), String.valueOf(now)
        );

        int r = result.intValue();
        // 3.判断结果
        if (r == 1) {
            throw new RuntimeException("库存不足");
        } else if(r == 2){
            throw new RuntimeException("每人限领一张");
        } else if(r == 3){
            throw new RuntimeException("活动未开始");
        } else if(r == 4){
            throw new RuntimeException("活动已结束");
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
                    System.out.println("Processing pending order: " + orderId+", "+ userId + ", " + cId);
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
            System.out.println("获取锁失败");
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
                
                // 2. 扣减库存 (Optimistic Lock)
                int success = couponMapper.updateStockDecrease(couponId);
                if (success < 1) {
                    //库存不足
                    //TODO:处理
                    return null;
                }
                
                // 3. Create Order
                // fetch coupon details for redundancy
                Coupon coupon = couponMapper.selectCouponById(couponId);
                if (coupon == null) {
                    throw new RuntimeException("Coupon not found during order creation");
                }

                UserCoupon userCoupon = new UserCoupon();
                userCoupon.setUserCouponId(orderId);
                userCoupon.setUserId(userId);
                userCoupon.setCouponId(couponId);
                userCoupon.setGetTime(LocalDateTime.now());
                userCoupon.setStatus(false); // Unused

                // Populate redundant fields
                userCoupon.setBizId(coupon.getBizId());
                userCoupon.setName(coupon.getName());
                userCoupon.setDiscountAmount(coupon.getDiscountAmount());
                userCoupon.setThresholdAmount(coupon.getThresholdAmount());
                userCoupon.setValidFrom(coupon.getValidFrom());
                userCoupon.setValidTo(coupon.getValidTo());

                if(userCouponMapper.insertUserCoupon(userCoupon) <= 0){
                    throw new RuntimeException("Failed to create user coupon record");
                }
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
        if(bizId == null || bizMapper.selectBusinessById(bizId) == null){
            throw new RuntimeException("商家不存在");
        }

        //2.如果是秒杀券，必须设置有效的秒杀时间（这里假设availableQuantity>0或者其他标志位，
        // 但根据现有逻辑，可用量初始等于总量，且通过addSeckillVoucher预热）
        
        //3.检查优惠券参数是否合法
        if(coupon.getName() == null || coupon.getName().trim().isEmpty()){
            throw new RuntimeException("优惠券名称不能为空");
        }
        if(coupon.getDiscountAmount() == null || coupon.getDiscountAmount() <= 0){
            throw new RuntimeException("优惠金额必须大于0");
        }
        if(coupon.getThresholdAmount() == null || coupon.getThresholdAmount() < 0){
            // 允许无门槛，但不能为负
             throw new RuntimeException("使用门槛金额不能为负");
        }
        if(coupon.getTotalQuantity() == null || coupon.getTotalQuantity() <= 0){
            throw new RuntimeException("发放总量必须大于0");
        }
        if(coupon.getValidFrom() == null || coupon.getValidTo() == null){
            throw new RuntimeException("请设置有效期");
        }
        if(coupon.getValidTo().isBefore(coupon.getValidFrom())){
            throw new RuntimeException("结束时间必须晚于开始时间");
        }
        if(coupon.getValidTo().isBefore(LocalDateTime.now())){
            throw new RuntimeException("结束时间必须在当前时间之后");
        }

        //4.设置默认值和补全信息
        String couponId = UUID.randomUUID().toString().replace("-", "");
        coupon.setCouponId(couponId);
        coupon.setBizId(bizId);
        coupon.setCreateTime(LocalDateTime.now());
        coupon.setStatus(1); // Default active (1)
        coupon.setAvailableQuantity(coupon.getTotalQuantity()); // Init stock
        
        if (coupon.getDescription() == null) {
            coupon.setDescription("");
        }

        //5.插入数据库
        couponMapper.insertCoupon(coupon);

        //6.如果活动已经开始或者即将开始（例如在5分钟内），可以选择直接预热到Redis
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom().isBefore(now.plusMinutes(5)) && coupon.getValidTo().isAfter(now)) {
            addSeckillVoucher(couponId);
        }
    }
    
    // 定时任务调用：预热近期开启的优惠券
    // 加载未来5分钟内开始的优惠券
    public void loadUpcomingCoupons() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(5); //提前5min
        List<Coupon> coupons = couponMapper.selectFutureValidCoupons(now, threshold);
        
        if (CollectionUtils.isEmpty(coupons)) {
            return;
        }
        
        for (Coupon coupon : coupons) {
            // Check if already loaded
            String key = "seckill:stock:" + coupon.getCouponId();
            Boolean hasKey = redisTemplate.hasKey(key);
            if (!hasKey) {
                addSeckillVoucher(coupon.getCouponId());
                System.out.println("Loaded voucher: " + coupon.getCouponId());
            }
        }
    }
    
    // 定时任务调用：清理过期优惠券缓存
    public void cleanExpiredCoupons() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredIds = couponMapper.selectExpiredCouponIds(now);
        
        if (CollectionUtils.isEmpty(expiredIds)) {
            return;
        }
        
        List<String> keysToDelete = new ArrayList<>();
        for (String id : expiredIds) {
            keysToDelete.add("seckill:stock:" + id);
            keysToDelete.add("seckill:order:" + id);
            keysToDelete.add("seckill:startTime:" + id);
            keysToDelete.add("seckill:endTime:" + id);
        }
        redisTemplate.delete(keysToDelete);
        System.out.println("Cleaned expired vouchers: " + expiredIds.size());
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
