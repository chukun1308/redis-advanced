package com.chukun.redis.controller;

import com.chukun.redis.service.RedPackageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 迷你版抢红包服务
 * @author chukun
 */
@RestController
@RequestMapping(value = "/red/package/")
public class RedPackageController {

    @Autowired
    private RedPackageService redPackageService;

    @GetMapping(value = "/send")
    public String  sendRedPackage(int totalMoney,int redPackageNumber) {
        return redPackageService.sendRedPackage(totalMoney, redPackageNumber);
    }

    @GetMapping(value = "/consume")
    public String  consumeRedPackage(String redPackage,String userId) {
        return redPackageService.consumeRedPackage(redPackage, userId);
    }

}
