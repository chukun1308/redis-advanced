package com.chukun.redis.service;

import com.chukun.redis.constant.Constants;
import com.chukun.redis.entities.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Service
public class ProductService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 查询redis缓存数据,单一缓存查询，方案会出现缓存击穿的问题，会使数据库的压力剧增
     * @param page
     * @param size
     * @return
     */
    public List<Product> queryProductsFromSingleCache(int page, int size) {
        List<Product> list=null;
        long start = (page - 1) * size;
        long end = start + size - 1;
        try {
            //采用redis list数据结构的lrange命令实现分页查询
            list = this.redisTemplate.opsForList().range(Constants.REDIS_HOT_PRODUCT, start, end);
            if (CollectionUtils.isEmpty(list)) {
                //TODO 走DB查询
                // redis失效的一瞬间，还是会打到数据库，造成数据库的压力剧增
            }
            log.info("查询结果：{}", list);
        } catch (Exception ex) {
            //这里的异常，一般是redis瘫痪 ，或 redis网络timeout
            log.error("exception:", ex);
            //TODO 走DB查询
        }
        return list;
    }
    /**
     * 查询redis缓存数据，双份缓存查询，方案也会出现缓存击穿的问题，但是不会使数据库的压力剧增
     * @param page
     * @param size
     * @return
     */
    public List<Product> queryProductsFromDoubleCache(int page, int size) {
        List<Product> list=null;
        long start = (page - 1) * size;
        long end = start + size - 1;
        try {
            //采用redis list数据结构的lrange命令实现分页查询
            list = this.redisTemplate.opsForList().range(Constants.REDIS_HOT_PRODUCT, start, end);
            if (CollectionUtils.isEmpty(list)) {
                // 再次查询redis缓存的备份数据
                list = this.redisTemplate.opsForList().range(Constants.REDIS_HOT_PRODUCT_BACKUP, start, end);
            }
            log.info("查询结果：{}", list);
        } catch (Exception ex) {
            //这里的异常，一般是redis瘫痪 ，或 redis网络timeout
            log.error("exception:", ex);
            //TODO 走DB查询
        }
        return list;
    }

}
