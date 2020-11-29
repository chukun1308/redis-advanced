package com.chukun.redis.controller;

import com.chukun.redis.config.RedisUtils;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
public class StockController {

    private static final String GOODS_KEY = "goods:001";

    private static final String REDIS_LOCK = "goods_lock";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private int serverPort;

    /**
     * 版本1：
     * 多线程情况下，会出现超卖的情况
     *
     * @return
     */
    @GetMapping("/buy/goods01")
    public String buyGoods01() {

        String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
        int goodsNumber = result == null ? 0 : Integer.parseInt(result);
        if (goodsNumber > 0) {
            int realNumber = goodsNumber - 1;
            stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
            System.out.println("成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort);
            return "成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort;
        } else {
            System.out.println("商品已经售罄...... ---> " + serverPort);
            return "商品已经售罄...... ---> " + serverPort;
        }
    }

    /**
     * 加线程锁，也就是jvm锁,此版本，在单机环境下，是不会出问题的
     * 但是，分布式情况下，由于会多实例，那么jvm锁只能保证单实例多线程情况下是安全的
     * 多进程就没法保证了，这时，就需要使用分布式锁解决此问题。
     *
     *  synchronized : 会造成线程挤压。
     *  ReentrantLock ：使用tryLock方法，不会造成线程挤压
     *
     * @return
     */
    @GetMapping("/buy/goods02")
    public String buyGoods02() {
        synchronized (this) {
            String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
                System.out.println("成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort);
                return "成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort;
            } else {
                System.out.println("商品已经售罄...... ---> " + serverPort);
                return "商品已经售罄...... ---> " + serverPort;
            }
        }
    }

    /**
     * 使用redis加分布式锁，此版本使用redis加锁
     * 会出现如下问题:
     * 代码出现异常，导致redis的删除锁的代码，压根不执行。
     * 解决思路：
     * 使用try finally块解决
     *
     * @return
     */
    @GetMapping("/buy/goods03")
    public String buyGoods03() {
        String lockVal = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, lockVal);
            if (!locked) {
                return "系统繁忙,请稍后重试";
            }
            String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String message;
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
                message = "成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort;
                System.out.println(message);
            } else {
                message = "商品已经售罄...... ---> " + serverPort;
                System.out.println(message);
            }
            return message;
        } finally {
            // 删除锁
            stringRedisTemplate.delete(REDIS_LOCK);
        }
    }

    /**
     * 此版本会出现如下问题:
     * 进程崩溃，进程被kill，redis挂了，导致redis的删除锁的代码，压根不执行。
     * 解决思路：
     * 给redis锁加一个过期时间
     *
     * @return
     */
    @GetMapping("/buy/goods04")
    public String buyGoods04() {
        String lockVal = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // 给redis锁，加10s的过期时间
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, lockVal, 10L, TimeUnit.SECONDS);
            if (!locked) {
                return "系统繁忙,请稍后重试";
            }
            String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String message;
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
                message = "成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort;
                System.out.println(message);
            } else {
                message = "商品已经售罄...... ---> " + serverPort;
                System.out.println(message);
            }
            return message;
        } finally {
            // 删除锁
            stringRedisTemplate.delete(REDIS_LOCK);
        }
    }

    /**
     * 此版本会出现如下问题:
     *  加锁加了10s的过期时间，而业务逻辑执行了20s，这时锁就会过期了，这个时候，其他的进程是会拿到这把锁，
     *  而此时当前进程处理完了，进行锁删除，那么，就会把其他进程的锁删除了
     * 解决思路：
     *   比较锁的value值，相等再删除
     *
     * @return
     */
    @GetMapping("/buy/goods05")
    public String buyGoods05() {
        String lockVal = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // 给redis锁，加10s的过期时间
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, lockVal, 10L, TimeUnit.SECONDS);
            if (!locked) {
                return "系统繁忙,请稍后重试";
            }
            String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String message;
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
                message = "成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort;
                System.out.println(message);
            } else {
                message = "商品已经售罄...... ---> " + serverPort;
                System.out.println(message);
            }
            return message;
        } finally {
            // 删除锁
            if (lockVal.equals(stringRedisTemplate.opsForValue().get(REDIS_LOCK))) {
                stringRedisTemplate.delete(REDIS_LOCK);
            }
        }
    }

    /**
     * 此版本会出现如下问题:
     *  比较锁的value值，与删除锁的逻辑，不是一个原子操作。
     *  例如：一种极端情况：
     *     在进入比较值的时候相等，这时会进去 if代码块，恰在这时，redis锁超时了，
     *     而其他进程获取了redis的这把锁，而当前进程执行删除操作，这时还是会把其他进程加的锁删掉
     * 解决思路：
     *   使用lua脚本原子删除。（推荐）
     *   使用redis的事务特性，删除锁
     *
     * @return
     */
    @GetMapping("/buy/goods06")
    public String buyGoods06() {
        String lockVal = UUID.randomUUID().toString() + Thread.currentThread().getName();
        try {
            // 给redis锁，加10s的过期时间
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(REDIS_LOCK, lockVal, 10L, TimeUnit.SECONDS);
            if (!locked) {
                return "系统繁忙,请稍后重试";
            }
            String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String message;
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
                message = "成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort;
                System.out.println(message);
            } else {
                message = "商品已经售罄...... ---> " + serverPort;
                System.out.println(message);
            }
            return message;
        } finally {
            // 使用lua删除redis锁
            deleteRedisLockWithLua(lockVal);
            // 使用事务删除redis锁
            // deleteRedisLockWithTransaction(lockVal);
        }
    }

    /**
     * 此版本会出现如下问题:
     *  1. redis锁过期怎么续租
     *  2. redis是AP模型，主从redis挂了，导致从节点上位，导致redis锁丢失
     *
     * 解决思路：
     *   使用 redisson解决上述问题，详见 https://redis.io/topics/distlock
     *
     * @return
     */
    @GetMapping("/buy/goods07")
    public String buyGoods07() {
        RLock redissonLock = RedisUtils.redissonClient().getLock(REDIS_LOCK);
        // 加锁
        redissonLock.lock();
        try {
            String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
            int goodsNumber = result == null ? 0 : Integer.parseInt(result);
            String message;
            if (goodsNumber > 0) {
                int realNumber = goodsNumber - 1;
                stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
                message = "成功买下商品，库存还剩下: " + realNumber + " ---> " + serverPort;
                System.out.println(message);
            } else {
                message = "商品已经售罄...... ---> " + serverPort;
                System.out.println(message);
            }
            return message;
        } finally {
            /**
             * 解锁,直接解锁在非常高并发的情况下，
             * 会出现 IllegalMonitorStateException：attempt to unlock lock,but locked by current thread by node id
              */
            // redissonLock.unlock();
            // 判断当前锁没释放，并且还是被当前线程占有
            if (redissonLock.isLocked() && redissonLock.isHeldByCurrentThread()) {
                redissonLock.unlock();
            }
        }
    }

    /**
     * 使用lua删除redis锁
     * @param lockVal
     * @return
     */
    private boolean deleteRedisLockWithLua(String lockVal)  {
        String delLockLua = "if redis.call(get,KEYS[1]) == ARGV[1]" +
                "then" +
                "    return redis.call(del,KEYS[1])" +
                "else" +
                "    return 0" +
                "end";
        Jedis jedis = null;
        try {
            jedis = RedisUtils.getJedis();
            Object result = jedis.eval(delLockLua, Collections.singletonList(REDIS_LOCK), Collections.singletonList(lockVal));
            return "1".equals(result.toString());
        }catch (Exception e) {
            // ignore exception
        }finally {
            if (!Objects.isNull(jedis)) {
                jedis.close();
            }
        }
        return false;
    }

    /**
     * 使用redis的事务删除redis锁
     * @param lockVal
     */
    private void deleteRedisLockWithTransaction(String lockVal)  {
        while (true) {
            stringRedisTemplate.watch(REDIS_LOCK);
            if (lockVal.equals(stringRedisTemplate.opsForValue().get(REDIS_LOCK))) {
                // 开启事务
                stringRedisTemplate.setEnableTransactionSupport(true);
                stringRedisTemplate.multi();
                stringRedisTemplate.delete(REDIS_LOCK);
                List<Object> list = stringRedisTemplate.exec();
                if (Objects.isNull(list)) {
                    continue;
                }
                stringRedisTemplate.unwatch();
                break;
            }
        }
    }
}
