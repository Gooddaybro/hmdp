package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@SuppressWarnings("all")
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
    /**
     * 根据id查询商铺缓存->解决缓存穿透
     *
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        //用缓解缓存击穿的方式
        // Shop shop=queryWithMutex(id);
        //逻辑缓存 缓解缓存击穿
       // Shop shop=queryWithLogicalExpire(id);
        //Shop shop=cacheClient.queryWithPassThrough(CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.SECONDS);
        //缓解缓存击穿-逻辑过期
        Shop shop=cacheClient.queryWithMutex(CACHE_SHOP_KEY,id,Shop.class,this::getById,20L,TimeUnit.SECONDS);
        if (shop==null) {
            return Result.fail("店铺不存在!");
        }
        return Result.ok(shop);
    }

    /**
     * 封装缓存穿透
     *
     * @param id
     * @return
     */
//    public Shop queryWithPassThrough(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        String shops = stringRedisTemplate.opsForValue().get(key);
//        //redis里有数据直接返回
//        if (StrUtil.isNotBlank(shops)) {
//            Shop shop1 = JSONUtil.toBean(shops, Shop.class, true);
//            return shop1;
//        }
//        //缓存穿透拦截->如果排到了这个地雷就直接给炸掉
//        if (shops != null) {
//            return null;
//        }
//        //查找数据库和埋地雷
//        Shop shop = getById(id);
//        if (shop == null) {
//            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//            return null;
//        }
//        //解决缓存雪崩->如果key同时失效引起的错误
//        String jsonStr = JSONUtil.toJsonStr(shop);
//        stringRedisTemplate.opsForValue().set(key, jsonStr, RedisConstants.CACHE_SHOP_TTL + RandomUtil.randomInt(1, 10), TimeUnit.MINUTES);
//        return shop;
//    }

    /**
     * 缓解缓存击穿
     * @param id
     * @return
     */
//    public Shop queryWithMutex(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        String shops = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isNotBlank(shops)) {
//            Shop shop1 = JSONUtil.toBean(shops, Shop.class, true);
//            return shop1;
//        }
//        if (shops!=null) {
//            return null;
//        }
//        String key1=RedisConstants.LOCK_SHOP_KEY+id;
//        Shop byId=null;
//        try {
//            boolean b = tryLock(key1);
//            if(!b){
//                //缺少休眠机制，不然会反复冲击redis
//                Thread.sleep(50);
//                return queryWithMutex(id);
//            }
//            //检查用的key调用错误
//            // String shops1 = stringRedisTemplate.opsForValue().get(key1);
//            String shops1 = stringRedisTemplate.opsForValue().get(key);
//            if(StrUtil.isNotBlank(shops1)){
//                Shop shop1 = JSONUtil.toBean(shops1, Shop.class, true);
//                return shop1;
//            }
//            if (shops1!=null) {
//                return null;
//            }
//            byId = getById(id);
//            //模拟重建延迟
//            Thread.sleep(200);
//            if (byId == null) {
//                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
//                return null;
//            }
//            String jsonStr = JSONUtil.toJsonStr(byId);
//            stringRedisTemplate.opsForValue().set(key, jsonStr, RedisConstants.CACHE_SHOP_TTL + RandomUtil.randomInt(1, 10), TimeUnit.MINUTES);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        } finally {
//            unlock(key1);
//        }
//        return byId;
//    }

    /**
     * 用物理期限缓解缓存击穿
     *
     * @param id
     * @return
     */
//    public Shop queryWithLogicalExpire(Long id) {
//        String key = CACHE_SHOP_KEY + id;
//        String shops = stringRedisTemplate.opsForValue().get(key);
//        if (StrUtil.isBlank(shops)) {
//            return null;
//        }
//        RedisData redisData= JSONUtil.toBean(shops,RedisData.class,true);
//        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class, true);
//        LocalDateTime expireTime = redisData.getExpireTime();
//        if(expireTime.isAfter(LocalDateTime.now())){
//            return shop;
//        }
//        String lockKey=RedisConstants.LOCK_SHOP_KEY+id;
//        boolean isLock = tryLock(lockKey);
//        if(isLock){
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    save2Redis(id,20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {
//                    unlock(lockKey);
//                }
//            });
//        }
//        return shop;
//    }


    /**
     * 根据id修改店铺->想修改数据库再删除缓存
     *
     * @param shop
     * @return
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("id为空");
        }
        String key = CACHE_SHOP_KEY + id;
        updateById(shop);
        stringRedisTemplate.delete(key);
        return Result.ok();
    }

    /**
     *  写入逻辑缓存预热
     *  从数据库中查找数据
     * @param id
     * @param expireSeconds
     * @return
     */
    public void save2Redis(Long id,Long expireSeconds) throws InterruptedException {
        Shop shop = getById(id);
        Thread.sleep(200);
        if (shop == null) {
            log.warn("预热失败，商铺ID {} 在数据库中不存在");
            return;
        }
        RedisData redisData=new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }

    /**
     * 防止缓存穿透上锁
     *
     * @param key
     * @return
     */
    public boolean tryLock(String key) {
        Boolean b = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(b);
    }

    /**
     * 解锁
     *
     * @param key
     */
    public void unlock(String key) {
        stringRedisTemplate.delete(key);

    }
}
