package com.chukun.redis.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * @author chukun
 * 文章点击量服务类
 */
@Service
public class ArticleService {

    public static final String ARTICLE_REDIS_PREFIX = "article:";

    /**
     * 文章点击上限值，一般动态化配置
     */
    private static final int THRESHOLD = 100000;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 高QPS使用会有问题，会直接将redis CPU打满
     * @param articleId
     * @return
     */
    public Long likeArticle(String articleId) {
        String key = ARTICLE_REDIS_PREFIX + articleId;
        return stringRedisTemplate.opsForValue().increment(key);
    }

    /**
     * 解决高QPS的问题
     * @param articleId
     * @return
     */
    public Long likeArticle02(String articleId) {
        String key = ARTICLE_REDIS_PREFIX + articleId;
        long articleNum = Long.parseLong(stringRedisTemplate.opsForValue().get(key));
        if (articleNum >=THRESHOLD) {
            // 代表显示 10W+，这种类似的效果
            return -1L;
        }
        return stringRedisTemplate.opsForValue().increment(key);
    }



}
