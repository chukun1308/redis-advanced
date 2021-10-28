package com.chukun.redis.service;

import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * redis多主高可用分布式锁
 * @author chukun
 */
@Slf4j
@Service
public class MultiRedisInstanceLock {

    public static final String LOCK_NAME = "multi_redis_lock";

    @Resource(name = "redissonClient01")
    private RedissonClient redissonClient01;

    @Resource(name = "redissonClient02")
    private RedissonClient redissonClient02;

    @Resource(name = "redissonClient03")
    private RedissonClient redissonClient03;

    /**
     * redis多主实例加锁，保证redis分布式锁，分布式环境的高可用
     */
    public void getlock() {
        RLock lock01 = redissonClient01.getLock(LOCK_NAME);
        RLock lock02 = redissonClient02.getLock(LOCK_NAME);
        RLock lock03 = redissonClient03.getLock(LOCK_NAME);

        // 3个实例同时加锁
        RedissonRedLock redissonRedLock = new RedissonRedLock(lock01, lock02, lock03);
        boolean isLockBoolean;
        try {
            //waitTime 抢锁的等待时间,正常情况下 等3秒
            //leaseTime就是redis key的过期时间,正常情况下等5分钟300秒。
            isLockBoolean = redissonRedLock.tryLock(3, 300, TimeUnit.SECONDS);
            log.info("线程{}，是否拿到锁：{} ",Thread.currentThread().getName(),isLockBoolean);
            if (isLockBoolean) {
                System.out.println(Thread.currentThread().getName()+"\t"+"---come in biz");
                //业务逻辑，忙10分钟
                try { TimeUnit.MINUTES.sleep(10); } catch (InterruptedException e) { e.printStackTrace(); }
            }
        } catch (Exception e) {
            log.error("redlock exception ",e);
        } finally {
            // 无论如何, 最后都要解锁
            redissonRedLock.unlock();
        }

    }
}
