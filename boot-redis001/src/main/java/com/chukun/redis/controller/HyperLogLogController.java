package com.chukun.redis.controller;

import com.chukun.redis.service.HyperLogLogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网站亿级用户UV统计
 * @author chukun
 */
@Api(description = "网站亿级用户UV统计")
@RestController
@Slf4j
public class HyperLogLogController {

    @Autowired
    private HyperLogLogService hyperLogLogService;

    @ApiOperation("获得IP去重后的首页访问量")
    @RequestMapping(value = "/uv/{pageId}",method = RequestMethod.GET)
    public long uv(@PathVariable("pageId") String pageId)
    {
        return hyperLogLogService.queryPageUserVisited(pageId);
    }
}
