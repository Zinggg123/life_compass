package com.zing.compass.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
public class TestController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @GetMapping("/test")
    public String test() {
        try {
            // 测试 MySQL
            Integer mysqlResult = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

            // 测试 Redis
            redisTemplate.opsForValue().set("test_key", "hello_redis");
            String redisValue = redisTemplate.opsForValue().get("test_key");

            return "✅ 连接成功！\n" +
                    "MySQL 正常，查询结果：" + mysqlResult + "\n" +
                    "Redis 正常，存入读取：" + redisValue;
        } catch (Exception e) {
            return "❌ 连接失败：" + e.getMessage();
        }
    }
}
