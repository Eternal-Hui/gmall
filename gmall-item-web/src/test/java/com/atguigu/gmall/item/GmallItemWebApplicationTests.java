package com.atguigu.gmall.item;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.service.PmsSkuService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallItemWebApplicationTests {

    @Reference
    PmsSkuService pmsSkuService;

    @Test
    public void contextLoads() {

        String[] ids = new String[3];
        ids[0] = "296";
        ids[1] = "299";
        ids[2] = "302";
        String skuId = pmsSkuService.checkSkuBySalesValueIds(ids);
        System.out.println(skuId);

    }

    @Test
    public void contextLoads2() {

        String[] ids = new String[3];
        ids[0] = "296";
        ids[1] = "299";
        ids[2] = "300";
        String skuId = pmsSkuService.checkSkuBySalesValueIdsTwo(ids);
        System.out.println(skuId);

    }

}
