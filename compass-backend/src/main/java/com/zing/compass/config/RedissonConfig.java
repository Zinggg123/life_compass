package com.zing.compass.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${spring.data.redis.password:}") // 没有密码就默认为空
    private String password;
    
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        // Use single server mode for simplicity, adjust address if needed
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setPassword(password.isBlank() ? null : password)
                .setConnectionPoolSize(10) //最多 10 个并发连接
                .setConnectionMinimumIdleSize(2) //最少2个空闲连接
                .setConnectTimeout(3000); //超时3s
        return Redisson.create(config);
    }
}

