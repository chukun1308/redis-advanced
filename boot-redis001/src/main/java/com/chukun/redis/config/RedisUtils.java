package com.chukun.redis.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Objects;

public class RedisUtils {

    private static final  JedisPool JEDIS_POOL;

    private static final RedissonClient redissonClient;


    static {
        JedisPoolConfig jedisPoolConfig  = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(20);
        jedisPoolConfig.setMaxIdle(20);
        JEDIS_POOL = new JedisPool(jedisPoolConfig);
    }

    static {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://linux01:6379").setDatabase(0);
        config.setCodec(new JsonJacksonCodec());
        redissonClient = Redisson.create(config);
    }

    /**
     * 普通客户端
     * @return
     */
    public static RedissonClient redissonClient() {
        return redissonClient;
    }

    public static Jedis getJedis() throws Exception {
        if (!Objects.isNull(JEDIS_POOL)) {
            return JEDIS_POOL.getResource();
        } else {
            throw new Exception("Can not get redis from pool");
        }
    }
}
