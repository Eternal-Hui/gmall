package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.PmsProductService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class PmsProductServiceImpl implements PmsProductService {

    @Autowired
    PmsProductInfoMapper pmsProductInfoMapper;

    @Autowired
    PmsBaseSaleAttrMapper pmsBaseSaleAttrMapper;

    @Autowired
    PmsProductSaleAttrMapper pmsProductSaleAttrMapper;

    @Autowired
    PmsProductSaleAttrValueMapper pmsProductSaleAttrValueMapper;

    @Autowired
    PmsProductImageMapper pmsProductImageMapper;

    @Override
    public List<PmsProductInfo> spuList(String catalog3Id) {

        PmsProductInfo pmsProductInfo = new PmsProductInfo();
        pmsProductInfo.setCatalog3Id(catalog3Id);
        List<PmsProductInfo> select = pmsProductInfoMapper.select(pmsProductInfo);

        return select;
    }

    @Override
    public List<PmsBaseSaleAttr> baseSaleAttrList() {
        return pmsBaseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(PmsProductInfo pmsProductInfo) {
        pmsProductInfoMapper.insertSelective(pmsProductInfo);

        // 获取插入成功返回的主键
        String productId = pmsProductInfo.getId();

        // 获取销售属性spuSaleAttrList
        List<PmsProductSaleAttr> spuSaleAttrList = pmsProductInfo.getSpuSaleAttrList();

        // 遍历PmsProductSaleAttr
        for (PmsProductSaleAttr pmsProductSaleAttr : spuSaleAttrList) {

            pmsProductSaleAttr.setProductId(productId);

            pmsProductSaleAttrMapper.insertSelective(pmsProductSaleAttr);
            // 获取销售属性值
            List<PmsProductSaleAttrValue> spuSaleAttrValueList = pmsProductSaleAttr.getSpuSaleAttrValueList();

            for (PmsProductSaleAttrValue pmsProductSaleAttrValue : spuSaleAttrValueList) {

                pmsProductSaleAttrValue.setProductId(productId);

                pmsProductSaleAttrValueMapper.insertSelective(pmsProductSaleAttrValue);

            }
        }

        // 保存图片
        // 获取图片URL集合
        List<PmsProductImage> spuImageList = pmsProductInfo.getSpuImageList();

        if (CollectionUtils.isNotEmpty(spuImageList)){

            // 遍历图片集合
            for (PmsProductImage pmsProductImage : spuImageList) {
                pmsProductImage.setProductId(productId);
                pmsProductImageMapper.insertSelective(pmsProductImage);
            }
        }

    }

    @Override
    public List<PmsProductSaleAttr> spuSaleAttrList(String spuId) {

        PmsProductSaleAttr pmsProductSaleAttr = new PmsProductSaleAttr();
        pmsProductSaleAttr.setProductId(spuId);

        // 根据spuId查询出对应的销售属性集合
        List<PmsProductSaleAttr> pmsProductSaleAttrs = pmsProductSaleAttrMapper.select(pmsProductSaleAttr);

        // 遍历销售属性集合
        for (PmsProductSaleAttr productSaleAttr : pmsProductSaleAttrs) {
            // 获取销售属性Id
            String saleAttrId = productSaleAttr.getSaleAttrId();

            PmsProductSaleAttrValue pmsProductSaleAttrValue = new PmsProductSaleAttrValue();

            pmsProductSaleAttrValue.setProductId(spuId);
            pmsProductSaleAttrValue.setSaleAttrId(saleAttrId);

            // 根据销售属性Id和平台属性id查询出销售属性值集合
            List<PmsProductSaleAttrValue> pmsProductSaleAttrValues = pmsProductSaleAttrValueMapper.select(pmsProductSaleAttrValue);

            // 把销售属性值集合设置到对应的销售属性中
            productSaleAttr.setSpuSaleAttrValueList(pmsProductSaleAttrValues);

        }

        return pmsProductSaleAttrs;
    }

    @Override
    public List<PmsProductImage> spuImageList(String spuId) {

        PmsProductImage pmsProductImage = new PmsProductImage();

        pmsProductImage.setProductId(spuId);

        return  pmsProductImageMapper.select(pmsProductImage);
    }


}
