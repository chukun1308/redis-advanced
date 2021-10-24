package com.chukun.redis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(value = "com.chukun.redis.mapper")
public class BootRedis001Application {

    public static void main(String[] args) {
        SpringApplication.run(BootRedis001Application.class, args);
    }

}
