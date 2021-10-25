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

/**
 * 加载商品的信息，到redis
 * @author chukun
 */
@Slf4j
@EnableScheduling
@Component
public class SingleCacheLoadProductTask {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 每隔1分钟加载一次
     * 此加载方式有问题，就是删除redis缓存，与加入redis缓存，不是原子操作，高并发下，热点数据，还是会击穿redis，打到数据库
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void loadProduct() {
        //模拟从数据库读取100件特价商品，用于加载到聚划算的页面中
        List<Product> list=this.products();
        //采用redis list数据结构的lpush来实现存储
        this.redisTemplate.delete(Constants.REDIS_HOT_PRODUCT);
        //lpush命令
        this.redisTemplate.opsForList().leftPushAll(Constants.REDIS_HOT_PRODUCT, list);
        log.info("runJhs定时刷新..............");
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
