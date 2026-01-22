package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 根据id查询商铺缓存
     * @param id
     * @return
     */
    @Override
    public Result queryById(Long id) {
        String key= RedisConstants.CACHE_SHOP_KEY+id;
        String shops = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(shops)){
            Shop shop1 = JSONUtil.toBean(shops, Shop.class, true);
            return Result.ok(shop1);
        }
        if(shops!=null){
            return Result.fail("店铺信息不存在!");
        }
        Shop shop=getById(id);
        if (shop==null) {
            stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("请求失败，店铺不存在");
        }
        String jsonStr = JSONUtil.toJsonStr(shop);
        stringRedisTemplate.opsForValue().set(key,jsonStr,RedisConstants.CACHE_SHOP_TTL+ RandomUtil.randomInt(1,10), TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    /**
     * 根据id修改店铺->想修改数据库再删除缓存
     * @param shop
     * @return
     */
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("id为空");
        }
        String key= RedisConstants.CACHE_SHOP_KEY+id;
        updateById(shop);
        stringRedisTemplate.delete(key);
        return Result.ok();
    }
}
