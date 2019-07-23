package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.PmsSkuAttrValueMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuImageMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuInfoMapper;
import com.atguigu.gmall.manage.mapper.PmsSkuSaleAttrValueMapper;
import com.atguigu.gmall.service.PmsSkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Service
public class PmsSkuServiceImpl implements PmsSkuService {

    @Autowired
    PmsSkuInfoMapper pmsSkuInfoMapper;
    @Autowired
    PmsSkuAttrValueMapper pmsSkuAttrValueMapper;
    @Autowired
    PmsSkuImageMapper pmsSkuImageMapper;
    @Autowired
    PmsSkuSaleAttrValueMapper pmsSkuSaleAttrValueMapper;

    @Autowired
    RedisUtil redisUtil;


    @Override
    public PmsSkuInfo getSkuInfoFromRedisBySkuId(String skuId){

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        // 获取Redis连接
        Jedis jedis = redisUtil.getJedis();
        try {
            // 从Redis中获取数据
            String skuJsonStr = jedis.get("sku:" + skuId + ":info");
            // 如果skuJsonStr为空,说明从Redis中取不到数据
            if (StringUtils.isBlank(skuJsonStr)){
                // 设置Redis分布式锁,过期时间为10秒
                String random = UUID.randomUUID().toString();
                String ok = jedis.set("sku:" + skuId + ":lock",random, "nx", "px", 10000);
                // 只有能设置成功说明拿到Redis,才去执行数据库查询
                if (StringUtils.isNotBlank(ok) && ok.equals("OK")){
                    // 从数据库中取
                    PmsSkuInfo skuInfoByIdFromDb = getSkuInfoById(skuId);
                    System.out.println("从数据库中拿数据");
                    if (skuInfoByIdFromDb != null){
                        // 存入Redis中
                        jedis.setex("sku:" + skuId + ":info",60*60*24*2,JSON.toJSONString(skuInfoByIdFromDb));

                        // 把Redis分布式锁删除
                        jedis.del("sku:" + skuId + ":lock");
                        // 把结果返回
                        return skuInfoByIdFromDb;
                    }
                }else{
                    // 没有拿到Redis锁的,开始自旋
                    return getSkuInfoFromRedisBySkuId( skuId);
                }
            }
            // 从Redis中获取到数据返回
            pmsSkuInfo = JSON.parseObject(skuJsonStr,PmsSkuInfo.class);
        } finally {
            jedis.close();
        }
        return pmsSkuInfo;
    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrListCheckedBySkuId(String skuId, String spuId){

        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsSkuSaleAttrValueMapper.selectSpuSaleAttrListCheckedBySkuId(skuId,spuId);
        return pmsProductSaleAttrs;
    }

    @Override
    public List<PmsSkuInfo> checkSkuBySpuId(String spuId) {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuSaleAttrValueMapper.selectCheckSkuBySpuId(spuId);

        return pmsSkuInfos;
    }

        @Override
    public String checkSkuBySalesValueIdsTwo(String[] ids){

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuSaleAttrValueMapper.selectCheckSkuBySalesValueIdsTwo(StringUtils.join(ids,","));

        HashMap<String, String> map = new HashMap<>();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();

            String saleAttrValueIdsStr = "";
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                saleAttrValueIdsStr = saleAttrValueIdsStr + "|" + pmsSkuSaleAttrValue.getSaleAttrValueId();
            }
            map.put(saleAttrValueIdsStr,skuId);
        }

        String attr_idsFromBrowserStr = "";
        for (String id : ids) {
            attr_idsFromBrowserStr = attr_idsFromBrowserStr + "|" + id;
        }

        String skuIdResult = map.get(attr_idsFromBrowserStr);


        return skuIdResult;
    }

    @Override
    public String checkSkuBySalesValueIds(String[] ids){
        String skuId = null;

        // 查询出所有包含这些销售属性值id数组对应的pmsSkuSaleAttrValues集合
        List<PmsSkuSaleAttrValue> pmsSkuSaleAttrValues = pmsSkuSaleAttrValueMapper.selectCheckSkuBySalesValueIds(StringUtils.join(ids,","));

        // 创建一个数组用于存放skuIds集合
        ArrayList<String> skuIds = new ArrayList<>();

        // 判断查询有对应的pmsSkuSaleAttrValues结果才往下执行
        if (CollectionUtils.isNotEmpty(pmsSkuSaleAttrValues)){

            // 遍历pmsSkuSaleAttrValues
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : pmsSkuSaleAttrValues) {
                // 取出每一个skuId,把当前skuId放进skuIds集合中
                skuIds.add(pmsSkuSaleAttrValue.getSkuId());
            }
        }

        HashMap<String, String> map = new HashMap<>();
        // 遍历skuIds
        for (String skuIdFromBrowser : skuIds) {
            String sku_attr_idsStr = "";
            // 再遍历一次pmsSkuSaleAttrValues,为了把相同的skuId对应的销售属性值id收集起来
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : pmsSkuSaleAttrValues) {
                String skuIdFromDB = pmsSkuSaleAttrValue.getSkuId();

                // 与skuId做比较是否相同
                if (skuIdFromBrowser.equals(skuIdFromDB)){
                    // 将销售属性值id做字符串拼接
                    sku_attr_idsStr = sku_attr_idsStr + "|" + pmsSkuSaleAttrValue.getSaleAttrValueId();
                }
            }
            // 将当前skuId对应的销售属性值Id集合字符串作为key值添加到hashMap中,与之相匹配的skuId作为value
            map.put(sku_attr_idsStr,skuIdFromBrowser);
        }
        System.out.println("数据库查出来的:"+map);
        // 这个是从客户端传过来要查询对应skuId的销售属性值id集合
        String attr_idsFromBrowserStr = "";
        for (String id : ids) {
            attr_idsFromBrowserStr = attr_idsFromBrowserStr + "|" + id;
        }
        System.out.println("要查询对应skuId的销售属性值id集合:"+attr_idsFromBrowserStr);

        String skuIdResult = map.get(attr_idsFromBrowserStr);

        if (StringUtils.isNotBlank(skuIdResult)){
            skuId = skuIdResult;
        }
        return skuId;
    }

    @Override
    public void saveSkuInfo(PmsSkuInfo pmsSkuInfo) {

        List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();

        String[] sale_attr_ids = new String[skuSaleAttrValueList.size()];

        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            sale_attr_ids[i] = skuSaleAttrValueList.get(i).getSaleAttrValueId();
        }
        // 判断数据库中是否已经有相同销售属性值组合的sku,不存在才添加
        if (checkSkuBySalesValueIds(sale_attr_ids) == null){

            pmsSkuInfoMapper.insertSelective(pmsSkuInfo);

            // 返回sku_Id
            String sku_Id = pmsSkuInfo.getId();

            // 保存skuAttrValueList
            // 获取skuAttrValueList
            List<PmsSkuAttrValue> skuAttrValueList = pmsSkuInfo.getSkuAttrValueList();

            for (PmsSkuAttrValue pmsSkuAttrValue : skuAttrValueList) {
                pmsSkuAttrValue.setSkuId(sku_Id);

                pmsSkuAttrValueMapper.insertSelective(pmsSkuAttrValue);
            }

            // 保存skuImageList
            // 获取skuImageList
            List<PmsSkuImage> skuImageList = pmsSkuInfo.getSkuImageList();

            for (PmsSkuImage pmsSkuImage : skuImageList) {
                pmsSkuImage.setSkuId(sku_Id);

                pmsSkuImageMapper.insertSelective(pmsSkuImage);
            }

            // 保存skuSaleAttrValueList
            // 获取skuSaleAttrValueList
           // List<PmsSkuSaleAttrValue> skuSaleAttrValueList = pmsSkuInfo.getSkuSaleAttrValueList();

            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                pmsSkuSaleAttrValue.setSkuId(sku_Id);

                pmsSkuSaleAttrValueMapper.insertSelective(pmsSkuSaleAttrValue);

            }

        }
    }

    @Override
    public PmsSkuInfo getSkuInfoById(String skuId) {

        PmsSkuInfo pmsSkuInfo = new PmsSkuInfo();
        pmsSkuInfo.setId(skuId);
        PmsSkuInfo skuInfo = pmsSkuInfoMapper.selectOne(pmsSkuInfo);

        // 获取skuId对应的图片集合
        PmsSkuImage pmsSkuImage = new PmsSkuImage();
        pmsSkuImage.setSkuId(skuId);
        List<PmsSkuImage> pmsSkuImages = pmsSkuImageMapper.select(pmsSkuImage);

        skuInfo.setSkuImageList(pmsSkuImages);

        return skuInfo;

    }

    @Override
    public List<PmsSkuInfo> getAllSku() {

        List<PmsSkuInfo> pmsSkuInfos = pmsSkuInfoMapper.selectAll();

        for (PmsSkuInfo pmsSkuInfo : pmsSkuInfos) {
            String skuId = pmsSkuInfo.getId();

           PmsSkuAttrValue pmsSkuAttrValue = new PmsSkuAttrValue();
           pmsSkuAttrValue.setSkuId(skuId);
           List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSkuAttrValueMapper.select(pmsSkuAttrValue);
           pmsSkuInfo.setSkuAttrValueList(pmsSkuAttrValues);
        }
        return pmsSkuInfos;
    }


}
