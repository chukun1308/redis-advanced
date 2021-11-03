package com.chukun.redis.service;

import com.chukun.redis.utils.ShortUrlUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 短链接生成，映射
 * @author chukun
 */
@Service
public class ShortUrlService {

    private static final String SHORT_URL_KEY = "short_url";

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 生成短链接
     * @param longUrl
     * @return
     */
    public  String encodeLongUrl(String longUrl) {
        // 先将长连接转为短链接
        String[] shortUrls = ShortUrlUtils.shortUrl(longUrl);
        //任意取出其中一个，我们就拿第一个
        String shortUrl = shortUrls[ThreadLocalRandom.current().nextInt(0, shortUrls.length)];
        // 存入redis
        redisTemplate.opsForHash().put(SHORT_URL_KEY, shortUrl, longUrl);
        return shortUrl;
    }

    /**
     * 根据短链接获取长连接
     * @param shortUrl
     * @return
     */
    public String decondeShortUrl(String shortUrl) {
        String longUrl = (String) redisTemplate.opsForHash().get(SHORT_URL_KEY, shortUrl);
        return longUrl;
    }

}
