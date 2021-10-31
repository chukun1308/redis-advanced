package com.chukun.redis.consistence;

import com.alibaba.fastjson.JSON;
import com.chukun.redis.config.RedisUtils;
import com.chukun.redis.entities.User;
import com.chukun.redis.service.UserService;
import io.netty.util.concurrent.CompleteFuture;
import org.apache.commons.lang.StringUtils;
import redis.clients.jedis.Jedis;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class CacheConsistence {

    private UserService userService = new UserService();

    /**
     * A线程先成功删除了redis里面的数据，然后去更新mysql，此时mysql正在更新中，还没有结束。（比如网络延时）
     * B突然出现要来读取缓存数据。
     * @param user
     */
    public void deleteUser(User user) {
         try(Jedis jedis = RedisUtils.getJedis()) {
             // 线程A先成功删除redis数据
             jedis.del(user.getId() +"");
             userService.updateUser(user);
             // 可能业务耗时
             TimeUnit.SECONDS.sleep(2);
         }catch (Exception e) {
             e.printStackTrace();
         }
    }

    /**
     * 先删除缓存，再更新数据库,采用延时双删策略 ,解决mysql数据与redis数据不一致的问题
     *
     * 延时双删会有以下问题?
     *   1.这个删除该休眠多久呢?
     *   2.当前演示的效果是mysql单机，如果mysql主从读写分离架构如何？
     *   3. 这种同步淘汰策略，吞吐量降低怎么办？
     * @param user
     */
    public void doubleDeleteUser(User user) {
        try(Jedis jedis = RedisUtils.getJedis()) {
            // 线程A先成功删除redis数据
            jedis.del(user.getId() +"");
            userService.updateUser(user);
            // 可能业务耗时
            TimeUnit.SECONDS.sleep(2);
            // 线程A在删除一次缓存
            jedis.del(user.getId() +"");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 先删除缓存，再更新数据库,采用延时双删策略 ,解决mysql数据与redis数据不一致的问题
     *
     * 延时双删会有以下问题?
     *   3. 这种同步淘汰策略，吞吐量降低怎么办？,如下代码，解决响应时间的问题
     * @param user
     */
    public void asyncDoubleDeleteUser(User user) {
        try(Jedis jedis = RedisUtils.getJedis()) {
            // 线程A先成功删除redis数据
            jedis.del(user.getId() +"");
            userService.updateUser(user);
            // 可能业务耗时
            TimeUnit.SECONDS.sleep(2);
            // 线程A在删除一次缓存
            CompletableFuture.supplyAsync(() -> {
                try {
                    TimeUnit.SECONDS.sleep(2);
                    return "ok";
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return "";
            }).whenComplete((u, throwable) -> {
                // 业务延迟执行完毕，在删除redis的缓存
                jedis.del(user.getId() +"");
            }).exceptionally((throwable) -> {
                 return "-1";
            }).get();
            jedis.del(user.getId() +"");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A线程先成功删除了redis里面的数据，然后去更新mysql，此时mysql正在更新中，还没有结束。（比如网络延时）
     *  B从mysql获得了旧值
     *        B线程发现redis里没有(缓存缺失)马上去mysql里面读取，从数据库里面读取来的是旧值。
     * 2.2 B会把获得的旧值写回redis
     *      获得旧值数据后返回前台并回写进redis(刚被A线程删除的旧数据有极大可能又被写回了)。
     * @param user
     */
    public User selectUser(User user) {
        try(Jedis jedis = RedisUtils.getJedis()) {
            // 先去redis查询缓存
            String result = jedis.get(user.getId() + "");
            if (StringUtils.isNotEmpty(result)) {
                return JSON.parseObject(result, User.class);
            } else {
                // B线程查询不到，查询数据库
                User selectUser = userService.queryUserById(user.getId());
                // 线程B 会将mysql的数据同步redis，还是旧值
                jedis.set(selectUser.getId() + "", JSON.toJSONString(selectUser));
                return selectUser;
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
