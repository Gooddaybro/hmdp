package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisWorker;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@Slf4j
@SpringBootTest
public class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopServiceimpl;

    @Resource
    private CacheClient cacheClient;


    @Resource
    private RedisWorker redisWorker;

    private ExecutorService executorService = Executors.newFixedThreadPool(500);

    @Test
    void testShopID() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable runnable = () -> {
            for (int i = 0; i < 100; i++) {
                long id=redisWorker.nextId("order");
                System.out.println("id="+id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for (int i = 0; i < 300; i++) {
            executorService.submit(runnable);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time="+(end-begin));
    }

    @Test
    void testSaveShop() throws InterruptedException {
     //   shopServiceimpl.save2Redis(1L,10L);
//        Shop shop = shopServiceimpl.getById(1L);
//        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY+1L,shop,10L, TimeUnit.SECONDS);
        LocalDateTime time=LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        long second=time.toEpochSecond(ZoneOffset.UTC);
        log.info("时间戳为："+second);
    }

}
