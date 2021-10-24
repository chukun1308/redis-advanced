package com.chukun.redis.service;

import cn.hutool.json.JSON;
import com.chukun.redis.entities.User;
import com.chukun.redis.mapper.UserMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * @author chukun
 */
@Service
public class UserService {

    private static final String USER_REDIS_KEY_PREFIX = "user:";

    @Resource
    private UserMapper userMapper;
    @Resource
    private RedisTemplate<String, Serializable> redisTemplate;

    /**
     * 添加user
     * @param user
     */
    public void addUser(User user) {
        int row = userMapper.insert(user);
        if (row > 0) {
            // 在查一次数据坤，查出user，保证user是数据库里面的数据
            user = userMapper.selectByPrimaryKey(user.getId());
            // 写入redis
            redisTemplate.opsForValue().set(wrapperRedisKey(user.getId()), user);
        }
    }

    /**
     * 更新user
     * @param user
     */
    public void updateUser(User user) {
        int row = userMapper.updateByPrimaryKey(user);
        if (row > 0) {
            // 在查一次数据坤，查出user，保证user是数据库里面的数据
            user = userMapper.selectByPrimaryKey(user.getId());
            // 写入redis
            redisTemplate.opsForValue().set(wrapperRedisKey(user.getId()), user);
        }
    }

    /**
     * 更新user
     * @param id
     */
    public void deleteUser(Integer id) {
        int row = userMapper.deleteByPrimaryKey(id);
        if (row > 0) {
            // 删除redis
            redisTemplate.delete(wrapperRedisKey(id));
        }
    }

    /**
     * 查询user, 这种情况，高QPS的查询，会出现缓存击穿，缓存穿透的相关问题
     * @param id
     * @return
     */
    public User queryUserById(Integer id) {
        User user = null;
        // 先查redis
        user = (User) redisTemplate.opsForValue().get(wrapperRedisKey(id));
        if (user !=null) {
            return user;
        }
        // 查询数据库
        user = userMapper.selectByPrimaryKey(id);
        if (user!=null) {
            // 更新redis
            redisTemplate.opsForValue().set(wrapperRedisKey(id), user);
        }
        return user;
    }

    /**
     * 查询user, 解决缓存击穿的问题
     * @param id
     * @return
     */
    public User queryUserById02(Integer id) {
        User user = null;
        // 先查redis
        user = (User) redisTemplate.opsForValue().get(wrapperRedisKey(id));
        if (user !=null) {
            return user;
        }
        // 查询数据库，高QPS的应用，瞬间所有的流量会打在数据库上，针对此情况，加锁处理
        synchronized (UserService.class) {
            // 在查询redis，看看是否已经有数据了
            user = (User) redisTemplate.opsForValue().get(wrapperRedisKey(id));
            if (user == null) {
                // 查询数据库
                user = userMapper.selectByPrimaryKey(id);
                if (user !=null) {
                    redisTemplate.opsForValue().setIfAbsent(wrapperRedisKey(id), user, 7, TimeUnit.DAYS);
                }
            }
        }
        return user;
    }



    private String wrapperRedisKey(Integer id) {
        return  USER_REDIS_KEY_PREFIX+id;
    }

}
