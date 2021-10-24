package com.chukun.redis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 附近酒店服务
 * @author chukun
 */
@Service
@Slf4j
public class GeoService {

    /**
     * redis的key前缀
     */
    public  static final String CITY ="city";

    @Resource
    private RedisTemplate<String, Serializable> redisTemplate;

    /**
     * 添加经纬度坐标
     * @return
     */
    public String geoAdd() {
        Map<Serializable, Point> map= new HashMap<>();
        map.put("天安门",new Point(116.403963,39.915119));
        map.put("故宫",new Point(116.403414 ,39.924091));
        map.put("长城" ,new Point(116.024067,40.362639));
        redisTemplate.opsForGeo().add(CITY, map);
        return map.toString();
    }

    /**
     * 获取位置的坐标信息
     * @param member
     * @return
     */
    public List<Point> position(String member) {
        //获取经纬度坐标
        List<Point> list= this.redisTemplate.opsForGeo().position(CITY,member);
        return list;
    }

    /**
     * 获取位置的Geo hash信息
     * @param member
     * @return
     */
    public List<String> hash(String member) {
        //geohash算法生成的base32编码值
        List<String> list= this.redisTemplate.opsForGeo().hash(CITY,member);
        return list;
    }

    /**
     * 获取两个位置之间的距离信息
     * @param member01
     * @param member02
     * @return
     */
    public Distance distance(String member01, String member02) {
        Distance distance= this.redisTemplate.opsForGeo().distance(CITY,member01,member02, RedisGeoCommands.DistanceUnit.KILOMETERS);
        return distance;
    }

    /**
     * 查询坐标半径范围内的信息，可以作为附件酒店的推送
     * @param lng
     * @param lat
     * @return
     */
    public GeoResults radiusByloc(String lng, String lat) {
        //这个坐标是北京王府井位置
        Point point = new Point(Double.parseDouble(lng), Double.parseDouble(lat));
        // 半径20公里内
        Distance distance = new Distance(20, Metrics.KILOMETERS);
        Circle circle = new Circle(point, distance);
        //返回50条
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().includeCoordinates().sortAscending().limit(50);
        GeoResults<RedisGeoCommands.GeoLocation<Serializable>> geoResults= this.redisTemplate.opsForGeo().radius(CITY,circle, args);
        return geoResults;
    }

    /**
     * 查询给定位置半径范围的信息
     * @param member
     * @return
     */
    public GeoResults radiusByMember(String member) {
        //返回50条
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().includeCoordinates().sortAscending().limit(50);
        //半径10公里内
        Distance distance=new Distance(10, Metrics.KILOMETERS);
        GeoResults<RedisGeoCommands.GeoLocation<Serializable>> geoResults= this.redisTemplate.opsForGeo().radius(CITY,member, distance,args);
        return geoResults;
    }

}
