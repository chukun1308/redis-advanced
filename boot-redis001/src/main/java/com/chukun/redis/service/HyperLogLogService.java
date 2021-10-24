package com.chukun.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * UV统计服务
 * @author chukun
 */
@Service
@Slf4j
public class HyperLogLogService {

    /**
     * 页面统计redis的key前缀
     */
    private static final String PAGE_UV_KEY = "index_page_uv:";

    private static final String JD_INDEX_PAGE_ID = "jd_index";

    @Resource
    private RedisTemplate<String, Serializable> redisTemplate;

    /**
     * 获取页面UV
     * @param pageId
     * @return
     */
    public long queryPageUserVisited(String pageId) {
        String key = PAGE_UV_KEY + pageId;
        return redisTemplate.opsForHyperLogLog().size(key);
    }


    /**
     * 模拟用户的点击操作
     */
    @PostConstruct
    public void mockData() {
        new Thread(() -> {
            Random random = new Random();
            String ip = null;
            for (int i = 0; i<=500; i++) {
                ip = random.nextInt(255) +"."+random.nextInt(255) +"."+random.nextInt(255) +"."+random.nextInt(255);
                redisTemplate.opsForHyperLogLog().add(PAGE_UV_KEY + JD_INDEX_PAGE_ID, ip);
                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    // ignored
                }
            }
        }, "mock-data").start();
    }

}
