package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    // 锁的前缀
    private static final String KEY_PREFIX = "lock:";
    // 锁的名称（用来区分不同的业务锁）
    private String name;
    private StringRedisTemplate stringRedisTemplate;

    // UUID 前缀，用于区分不同 JVM 的线程
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    // 初始化 Lua 脚本
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示，加上 UUID 前缀保证集群下的唯一性
        String threadId = ID_PREFIX + Thread.currentThread().getName();
        // 获取锁，使用 Redis 的 SETNX 命令 (setIfAbsent)
        // SET lock:name threadId EX timeoutSec NX
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.SECONDS);
        // 使用 Boolean.TRUE.equals 防止拆箱引发的 NullPointerException
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 调用 Lua 脚本进行判断和删除操作，保证原子性
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX + name),
                ID_PREFIX + Thread.currentThread().getName());
    }
}
