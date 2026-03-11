package com.hmdp.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        // 1. 创建配置对象
        Config config = new Config();
        // 2. 添加单机 Redis 配置
        // 这里的地址格式必须是以 redis:// 或者 rediss:// (如果是 SSL) 开头
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");
        // 若您的 Redis 有密码，需要加上 .setPassword("您的密码")

        // 3. 根据配置创建并返回 RedissonClient 实例
        return Redisson.create(config);
    }
}
