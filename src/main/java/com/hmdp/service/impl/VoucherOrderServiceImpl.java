package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.utils.RedisWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
        implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;

    @Resource
    private RedisWorker redisWorker;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 实现优惠券秒杀下单
     * 
     * @param voucherId
     * @return
     */
    @Override
    public Result seckillVoucher(Long voucherId) {

        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀尚未开始!");
        }

        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束!");
        }

        if (voucher.getStock() < 1) {
            return Result.fail("库存不足!");
        }

        Long userId = UserHolder.getUser().getId();

        // 【改动点】：使用 Redisson 提供的高级分布式可重入锁
        // 1. 获取锁对象 (RLock)，同样指定一个互斥的名称
        RLock lock = redissonClient.getLock("lock:order:" + userId);

        // 2. 尝试获取锁
        // 第一个参数：获取锁的最大等待时间（期间会不断重试），设为 0 表示不等待立刻返回结果
        // 第二个参数：锁自动释放的时间（如果传 -1，会触发 WatchDog 看门狗机制自动续期！）
        // 第三个参数：时间单位
        boolean isLock;
        try {
            isLock = lock.tryLock(0, -1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("尝试获取Redisson锁被中断", e);
            return Result.fail("系统异常，请稍后再试");
        }

        // 3. 如果获取锁失败，说明有重复提交，直接返回错误
        if (!isLock) {
            return Result.fail("不允许重复下单！");
        }

        // 4. 获取锁成功，执行业务逻辑
        try {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        } finally {
            // 5. 【关键】使用 Redisson 内置的释放锁方法
            lock.unlock();
        }

    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 一人一单
        Long userId = UserHolder.getUser().getId();
        Long count = query().eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (count > 0) {
            return Result.fail("用户已经买过一次!");
        }

        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            return Result.fail("库存不足！");
        }
        // 5. 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 5.1 生成唯一的订单 ID（调用你之前写的 RedisIdWorker）
        long orderId = redisWorker.nextId("order");
        voucherOrder.setId(orderId);
        // 5.2 用户 ID（从 UserHolder 中获取当前登录用户）
        voucherOrder.setUserId(UserHolder.getUser().getId());
        // 5.3 代金券 ID
        voucherOrder.setVoucherId(voucherId);
        // 5.4 保存到数据库
        save(voucherOrder);
        // 6. 返回订单 ID
        return Result.ok(orderId);
    }
}
