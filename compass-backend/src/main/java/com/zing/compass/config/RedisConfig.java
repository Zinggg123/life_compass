package com.zing.compass.config;

import com.alibaba.fastjson2.support.spring6.data.redis.FastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

//    @Bean
//    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
//        RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(factory);
//
//        // Key 序列化：字符串
//        StringRedisSerializer stringSerializer = new StringRedisSerializer();
//
//        // ✅ Value 序列化：使用 Fastjson2（你要的版本）
//        FastJsonRedisSerializer<Object> fastJsonSerializer = new FastJsonRedisSerializer<>(Object.class);
//
//        // 全局设置
//        template.setKeySerializer(stringSerializer);
//        template.setValueSerializer(fastJsonSerializer);
//        template.setHashKeySerializer(stringSerializer);
//        template.setHashValueSerializer(fastJsonSerializer);
//
//        template.afterPropertiesSet();
//        return template;
//    }
}
