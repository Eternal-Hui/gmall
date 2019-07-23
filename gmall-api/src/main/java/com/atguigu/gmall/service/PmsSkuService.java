package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;

import java.util.List;

public interface PmsSkuService {
    PmsSkuInfo getSkuInfoFromRedisBySkuId(String skuId);

    List<PmsProductSaleAttr> spuSaleAttrListCheckedBySkuId(String skuId, String spuId);

    List<PmsSkuInfo> checkSkuBySpuId(String spuId);

    String checkSkuBySalesValueIdsTwo(String[] ids);

    String checkSkuBySalesValueIds(String[] ids);

    void saveSkuInfo(PmsSkuInfo pmsSkuInfo);

    PmsSkuInfo getSkuInfoById(String skuId);

    List<PmsSkuInfo> getAllSku();
}
