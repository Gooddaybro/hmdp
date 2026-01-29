package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisWorker {
    private static final long BEGIN_TIMESTAMP =1767225600;
    private static final int  COUNT_BITS = 32;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 封装全局唯一ID
     * @param keyPrefix
     * @return
     */
    public long nextId(String keyPrefix){
        LocalDateTime now = LocalDateTime.now();
        long newSeconds=now.toEpochSecond(ZoneOffset.UTC);
        long timeStamp=newSeconds-BEGIN_TIMESTAMP;
        String date = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        // 这里的 key 格式类似 icr:order:2026:01:28
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + date);
        return timeStamp << COUNT_BITS | count;
    }

}
