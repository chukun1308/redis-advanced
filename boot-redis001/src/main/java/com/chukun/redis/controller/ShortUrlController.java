package com.chukun.redis.controller;

import com.chukun.redis.service.ShortUrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 迷你版抢红包服务
 * @author chukun
 */
@RestController
@RequestMapping(value = "/short/url/")
public class ShortUrlController {

    @Autowired
    private ShortUrlService shortUrlService;

    /**
     * 长链接转换为短链接
     * 实现原理：长链接转换为短加密串key，然后存储在redis的hash结构中。
     */
    @GetMapping("/encode/{longUrl}")
    public String encodeLongUrl(@PathVariable("longUrl") String longUrl) {
        return "http://127.0.0.1:5555/" + shortUrlService.encodeLongUrl(longUrl);
    }

    /**
     * 重定向到原始的URL
     * 实现原理：通过短加密串KEY到redis找出原始URL，然后重定向出去
     */
    @GetMapping("/decode/{shortUrl}")
    public void decodeLongUrl(@PathVariable("shortUrl") String shortUrl, HttpServletResponse response) {
        //到redis中把原始url找出来
        String longUrl = shortUrlService.decondeShortUrl(shortUrl);
        try {
            // 重定向到长链接地址
            response.sendRedirect(longUrl);
        } catch (IOException e) {
            // ignored
        }

    }
}
