package com.chukun.redis.controller;

import com.chukun.redis.service.ArticleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @auther chukun
 * @create 2021-05-01 19:55
 */
@RestController
@Slf4j
@Api(description = "喜欢的文章接口")
public class ArticleController
{
    @Resource
    private ArticleService articleService;

    @ApiOperation("喜欢的文章，点一次加一个喜欢")
    @RequestMapping(value ="/view/{articleId}", method = RequestMethod.POST)
    public Long likeArticle(@PathVariable(name="articleId") String articleId)
    {
        return articleService.likeArticle(articleId);
    }
}
