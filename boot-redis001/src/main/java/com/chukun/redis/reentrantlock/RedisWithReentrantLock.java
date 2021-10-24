package com.chukun.redis.reentrantlock;

import com.chukun.redis.config.RedisUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.HashMap;
import java.util.Map;

/**
 * redis 实现可重入锁
 */
public class RedisWithReentrantLock {

    private ThreadLocal<Map<String, Integer>> lockers = new ThreadLocal<>();

    /**
     * redis 加锁操作
     * @param key
     */
    private boolean _lock(String key) {
        Jedis jedis = null;
        try {
            jedis = RedisUtils.getJedis();
            SetParams params = new SetParams();
            params.nx().ex(5);
            return "ok".equals(jedis.set(key, "", params));
        }catch (Exception e) {
            // ignored
            return false;
        }
    }


    /**
     * redis 解锁操作
     * @param key
     */
    private void  _unlock(String key) {
        Jedis jedis = null;
        try {
            jedis = RedisUtils.getJedis();
            jedis.del(key);
        }catch (Exception e) {
            // ignored
        }
    }

    /**
     * 获取当前线程的私有存储
     * @return
     */
    private Map<String,Integer> currentLockers() {
        Map<String, Integer> refs = lockers.get();
        if (refs != null) {
            return refs;
        }
        lockers.set(new HashMap<>());
        return lockers.get();
    }

    /**
     * redis实现的可重入锁的加锁操作
     * @param key
     * @return
     */
    public boolean lock(String key) {
        Map<String, Integer> refs = currentLockers();
        Integer refCnt = refs.get(key);
        if (refCnt !=null) {
            refs.put(key, refCnt +=1);
        }
        boolean ok = this._lock(key);
        if (!ok) {
            return false;
        }
        refs.put(key, 1);
        return  true;
    }

    /**
     * redis实现的可重入锁的解锁操作
     * @param key
     * @return
     */
    public boolean unlock(String key) {
        Map<String, Integer> refs = currentLockers();
        Integer refCnt = refs.get(key);
        if (refCnt ==null) {
            return false;
        }
        if (refCnt > 0) {
            refs.put(key, refCnt -=1);
        } else {
            refs.remove(key);
            this._unlock(key);
        }
        return true;
    }
}
