package com.chukun.redis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController {

    private static final String GOODS_KEY="goods:001";
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Value("${server.port}")
    private int serverPort;

    @GetMapping("/buy/goods")
    public String buyGoods() {

        String result = stringRedisTemplate.opsForValue().get(GOODS_KEY);
        int goodsNumber = result==null? 0:Integer.parseInt(result);
        if (goodsNumber >0 ) {
            int realNumber = goodsNumber - 1;
            stringRedisTemplate.opsForValue().set(GOODS_KEY, String.valueOf(realNumber));
            System.out.println("成功买下商品，库存还剩下: " + realNumber + " ---> "+ serverPort);
            return "成功买下商品，库存还剩下: " + realNumber + " ---> "+ serverPort;
        } else {
            System.out.println("商品已经售罄...... ---> "+ serverPort);
            return "商品已经售罄...... ---> "+ serverPort;
        }
    }

}
