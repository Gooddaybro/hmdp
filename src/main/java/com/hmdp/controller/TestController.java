package com.hmdp.controller;

import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;

@Slf4j
@RestController
public class TestController {

    @Resource
    private RedissonClient redissonClient;

//    @GetMapping("/test-watchdog")
//    public String testWatchDog() {
//        // 1. 拿到一把名为 "dog_test_lock" 的锁
//        RLock lock = redissonClient.getLock("dog_test_lock");
//
//        // 2. 尝试加锁。
//        // 【关键点】：这里使用无参的 lock() 或 tryLock()，千万不要传 leaseTime（过期时间）参数！
//        // 如果你传了明确的过期时间，比如 tryLock(10, 10, TimeUnit.SECONDS)，看门狗机制就会失效。
//        lock.lock();
//
//        try {
//            log.info("\"========== 成功拿到锁，开始模拟 50 秒的超长业务 ==========\"");
//            // 3. 强行让线程睡死 50 秒，模拟业务极其卡顿
//            Thread.sleep(50000);
//            log.info("\"========== 50 秒结束，业务艰难执行完毕 ==========\"");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } finally {
//            // 4. 释放锁
//            lock.unlock();
//            log.info("\"========== 锁已手动释放 ==========\"");
//
//        }
//
//        return "实验结束";
//    }
}