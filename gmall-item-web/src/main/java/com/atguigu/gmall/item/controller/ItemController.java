package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import com.atguigu.gmall.service.PmsProductService;
import com.atguigu.gmall.service.PmsSkuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    PmsSkuService pmsSkuService;

    @Reference
    PmsProductService pmsProductService;

    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, ModelMap modelMap){

       // PmsSkuInfo pmsSkuInfo = pmsSkuService.getSkuInfoById(skuId);
        PmsSkuInfo pmsSkuInfo = pmsSkuService.getSkuInfoFromRedisBySkuId(skuId);

        modelMap.put("skuInfo",pmsSkuInfo);
        // 获取当前sku对应的销售属性值列表
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsSkuService.spuSaleAttrListCheckedBySkuId(skuId, pmsSkuInfo.getProductId());
        modelMap.put("spuSaleAttrListCheckBySku",pmsProductSaleAttrs);

        // 隐藏的当前sku所在的spu所有的sku销售属性值id的组合
        List<PmsSkuInfo> pmsSkuInfos = pmsSkuService.checkSkuBySpuId(pmsSkuInfo.getProductId());
        // 将sku销售属性值id的组合作为key,skuId作为value制作hash表
        HashMap<String, String> saleAttrValueIdMap = new HashMap<>();

        for (PmsSkuInfo skuInfo : pmsSkuInfos) {
            String skuIdForHash = skuInfo.getId();
            List<PmsSkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

            String saleAttrValueIdsStr = "";
            for (PmsSkuSaleAttrValue pmsSkuSaleAttrValue : skuSaleAttrValueList) {
                saleAttrValueIdsStr = saleAttrValueIdsStr + "|" + pmsSkuSaleAttrValue.getSaleAttrValueId();
            }
            saleAttrValueIdMap.put(saleAttrValueIdsStr,skuIdForHash);
        }
        modelMap.put("saleAttrValueIdMap", JSON.toJSON(saleAttrValueIdMap));

        return "item";
    }

}
