package com.chukun.redis.bloomfilter;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.util.ArrayList;
import java.util.List;

/**
 * Guava 中布隆过滤器,实现单机版的布隆过滤器
 * @author chukun
 */
public class GuavaBloomfilter {

    private static BloomFilter<Integer> guavaBloomFilter = null;

    private static final int _1W = 10000;

    private static final int size = 100 * _1W;

    //误判率,它越小误判的个数也就越少
    /**
     * Guava,默认的误判率是 0.03
     * 这个误判率不是设置的越小越好，因为设置的越小，使用的hash越多，占用的bit位也越多，影响执行效率，耗费CPU资源
     * 实际项目中要综合考量
     */
    public static double fpp = 0.03;

    static  {
        guavaBloomFilter = BloomFilter.create(Funnels.integerFunnel(), size);
    }

    /**
     * 执行布隆过滤器逻辑
     */
    public static void  executeLogic() {
        for (int i=0; i<size; i++) {
            guavaBloomFilter.put(i);
        }

        List<Integer> sample = new ArrayList<>(size);
        for (int i=0; i<size; i++) {
            if (guavaBloomFilter.mightContain(i)) {
                System.out.println(i+"\t"+"被误判了.");
                sample.add(i);
            }
        }

        System.out.println("------------------------------------");

        //故意取10万个不在过滤器里的值，看看有多少个会被认为在过滤器里
        List<Integer> list = new ArrayList<>(10 * _1W);
        for (int i = size+1; i < size + 100000; i++) {
            if (guavaBloomFilter.mightContain(i)) {
                System.out.println(i+"\t"+"被误判了.");
                list.add(i);
            }
        }
        System.out.println("误判的数量：" + list.size());
    }

    public static void main(String[] args) {
        executeLogic();
    }

}
