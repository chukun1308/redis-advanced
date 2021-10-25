package com.chukun.redis.task;

import com.chukun.redis.constant.Constants;
import com.chukun.redis.entities.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 加载商品的信息，到redis
 *
 * @author chukun
 */
@Slf4j
@EnableScheduling
@Component
public class DoubleCacheLoadProductTask {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 每隔1分钟加载一次
     * 使用双份redis缓存，存储热点数据，并且把失效时间差异化，以此达到redis缓存不会被击穿
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void loadProduct() {
        //模拟从数据库读取100件特价商品，用于加载到聚划算的页面中
        List<Product> list=this.products();
        //采用redis list数据结构的lpush来实现存储
        /**
         * 先存储备份的缓存数据，设置的失效时间长一点
         */
        this.redisTemplate.delete(Constants.REDIS_HOT_PRODUCT_BACKUP);
        //lpush命令
        this.redisTemplate.opsForList().leftPushAll(Constants.REDIS_HOT_PRODUCT_BACKUP, list);
        this.redisTemplate.expire(Constants.REDIS_HOT_PRODUCT_BACKUP, 15, TimeUnit.DAYS);

        /**
         * 在存储商品正常的缓存数据
         */
        this.redisTemplate.delete(Constants.REDIS_HOT_PRODUCT);
        //lpush命令
        this.redisTemplate.opsForList().leftPushAll(Constants.REDIS_HOT_PRODUCT, list);
        this.redisTemplate.expire(Constants.REDIS_HOT_PRODUCT, 10, TimeUnit.DAYS);
        log.info("定时刷新..............");
    }

    /**
     * 模拟从数据库读取20件特价商品，用于加载到聚划算的页面中
     */
    public List<Product> products() {
        List<Product> list=new ArrayList<>();
        for (int i = 1; i <=20; i++) {
            Random rand = new Random();
            int id= rand.nextInt(10000);
            Product obj=new Product((long) id,"product"+i,i,"detail");
            list.add(obj);
        }
        return list;
    }
}
