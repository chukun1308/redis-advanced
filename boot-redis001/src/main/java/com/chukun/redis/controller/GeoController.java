package com.chukun.redis.controller;

import com.chukun.redis.service.GeoService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 美团附近酒店推送
 * @author chukun
 */
@Api(description = "美团附近酒店推送")
@RestController
@Slf4j
public class GeoController {

    @Autowired
    private GeoService geoService;

    @RequestMapping("/geoadd")
    public String geoAdd()
    {
       return geoService.geoAdd();
    }

    @GetMapping(value = "/geopos")
    public List<Point> position(String member) {
        //获取经纬度坐标
        return geoService.position(member);
    }

    @GetMapping(value = "/geohash")
    public List<String> hash(String member) {
        //geohash算法生成的base32编码值
        return geoService.hash(member);
    }

    @GetMapping(value = "/geodist")
    public Distance distance(String member01, String member02) {
        return geoService.distance(member01, member02);
    }

    /**
     * 通过经度，纬度查找附近的
     * 北京王府井位置116.418017,39.914402
     */
    @GetMapping(value = "/georadius")
    public GeoResults radiusByxy() {
        //这个坐标是北京王府井位置
        return geoService.radiusByloc("116.418017", "39.914402");
    }

    /**
     * 通过地方查找附近
     */
    @GetMapping(value = "/georadiusByMember")
    public GeoResults radiusByMember() {
        //这个坐标是北京王府井位置
        return geoService.radiusByMember("天安门");
    }

}
