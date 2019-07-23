package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.PmsProductSaleAttr;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.bean.PmsSkuSaleAttrValue;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface PmsSkuSaleAttrValueMapper extends Mapper<PmsSkuSaleAttrValue> {
    List<PmsSkuSaleAttrValue> selectCheckSkuBySalesValueIds(@Param("sale_attr_ids") String sale_attr_ids);

    List<PmsSkuInfo> selectCheckSkuBySalesValueIdsTwo(@Param("sale_attr_ids") String sale_attr_ids);

    List<PmsSkuInfo> selectCheckSkuBySpuId(@Param("spuId") String spuId);

    List<PmsProductSaleAttr> selectSpuSaleAttrListCheckedBySkuId(@Param("skuId") String skuId,@Param("spuId") String spuId);
}
