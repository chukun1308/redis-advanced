package com.chukun.redis.service;

import cn.hutool.core.util.IdUtil;
import com.google.common.primitives.Ints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 迷你版抢红包
 * @author chukun
 */
@Slf4j
@Service
public class RedPackageService {

    private static final String RED_PACKAGE_PREFIX = "red_package_";

    private static final String RED_PACKAGE_CONSUME_PREFIX = "red_package_consume_";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 发红包实现
     * @param totalMoney
     * @param redPackageNumber
     * @return
     */
    public String sendRedPackage(int totalMoney,int redPackageNumber) {
        //1 拆红包，总金额拆分成多少个红包，每个小红包里面包多少钱
        Integer[] splitRedPackages = splitRedPackage(totalMoney, redPackageNumber);
        //2 红包的全局ID
        String key = RED_PACKAGE_PREFIX+ IdUtil.simpleUUID();
        //3 采用list存储红包并设置过期时间
        redisTemplate.opsForList().leftPushAll(key,splitRedPackages);
        redisTemplate.expire(key,1, TimeUnit.DAYS);
        return key+"\t"+"\t"+ Ints.asList(Arrays.stream(splitRedPackages).mapToInt(Integer::valueOf).toArray());
    }

    /**
     * 抢红包实现逻辑
     * @param redPackageKey
     * @param userId
     * @return
     */
    public String consumeRedPackage(String redPackageKey,String userId) {
        //1 验证某个用户是否抢过红包
        boolean isConsumed = redisTemplate.opsForHash().hasKey(RED_PACKAGE_CONSUME_PREFIX + redPackageKey, userId);
        //2 没有抢过就开抢，否则返回-2表示抢过
        if (!isConsumed) {
            // 2.1 从list里面出队一个红包，抢到了一个
            Object partRedPackage = redisTemplate.opsForList().leftPop(RED_PACKAGE_PREFIX + redPackageKey);
            if (partRedPackage != null) {
                //2.2 抢到手后，记录进去hash表示谁抢到了多少钱的某一个红包
                redisTemplate.opsForHash().put(RED_PACKAGE_CONSUME_PREFIX + redPackageKey,userId,partRedPackage);
                System.out.println("用户: "+userId+"\t 抢到多少钱红包: "+partRedPackage);
                //TODO 后续异步进mysql或者tMQ进一步处理,处理超时退还余额到原账户，红包记录等功能
                return String.valueOf(partRedPackage);
            }
            //抢完
            return "-1";
        }
        //3 某个用户抢过了，不可以作弊重新抢
        return "-2";
    }


    /**
     * 拆完红包总金额+每个小红包金额别太离谱
     * 使用二倍均值法拆红包
     * @param totalMoney
     * @param redPackageNumber
     * @return
     */
    private Integer[] splitRedPackage(int totalMoney, int redPackageNumber) {
        int useMoney = 0;
        Integer[] redPackageNumbers = new Integer[redPackageNumber];
        Random random = new Random();

        for (int i = 0; i < redPackageNumber; i++) {
            if(i == redPackageNumber - 1) {
                redPackageNumbers[i] = totalMoney - useMoney;
            }else{
                int avgMoney = (totalMoney - useMoney) * 2 / (redPackageNumber - i);
                redPackageNumbers[i] = 1 + random.nextInt(avgMoney - 1);
            }
            useMoney = useMoney + redPackageNumbers[i];
        }
        return redPackageNumbers;
    }


}
