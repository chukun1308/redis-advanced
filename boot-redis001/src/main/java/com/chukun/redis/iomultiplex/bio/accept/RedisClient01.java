package com.chukun.redis.iomultiplex.bio.accept;

import java.io.IOException;
import java.net.Socket;

/**
 * @auther chukun
 */
public class RedisClient01
{
    public static void main(String[] args) throws IOException
    {
        System.out.println("------RedisClient01 start");
        Socket socket = new Socket("127.0.0.1", 6379);


    }
}
