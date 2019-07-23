package com.atguigu.gmall.seckillweb.controller;

import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

@RestController
public class SeckillController {

    @Autowired
    RedisUtil redisUtil;

    @RequestMapping("kill")
    public String kill(){

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String stock = jedis.get("skuId:1:stock");
            if(Long.parseLong(stock) > 0){
                Transaction multi = jedis.multi();
                multi.watch("skuId:1:stock");
                multi.incrBy("skuId:1:stock",-1);

                List<Object> exec = multi.exec();

                if (CollectionUtils.isNotEmpty(exec)){
                    Long s = (Long) exec.get(0);
                    if (s > 0 ){
                        System.out.println("抢购成功!库存数量:" + stock);
                        return "抢购成功!库存数量:" + stock;
                    }else {
                        System.out.println("抢购失败...");
                        return "抢购失败...";
                    }
                }else {
                    System.out.println("抢购活动已结束");
                    return "抢购活动已结束!";
                }
            }

        } finally {
            jedis.close();
        }
        return "抢购活动已结束!";
    }

}