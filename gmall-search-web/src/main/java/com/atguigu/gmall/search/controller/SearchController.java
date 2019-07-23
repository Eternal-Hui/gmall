package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.PmsBaseAttrService;
import com.atguigu.gmall.service.PmsSearchService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

@Controller
public class SearchController {

    @Reference
    PmsSearchService pmsSearchService;

    @Reference
    PmsBaseAttrService pmsBaseAttrService;

    @RequestMapping("list.html")
    public String list(PmsSearchParam pmsSearchParam, ModelMap modelMap){

        List<PmsSearchSkuInfo> pmsSearchSkuInfos = pmsSearchService.search(pmsSearchParam);

        // 获取所有商品的平台属性值Id并去重
        HashSet<String> attrValueIdsSet = new HashSet<>();
        for (PmsSearchSkuInfo pmsSearchSkuInfo : pmsSearchSkuInfos) {
            List<PmsSkuAttrValue> pmsSkuAttrValues = pmsSearchSkuInfo.getSkuAttrValueList();
            for (PmsSkuAttrValue pmsSkuAttrValue : pmsSkuAttrValues) {
                String valueId = pmsSkuAttrValue.getValueId();
                attrValueIdsSet.add(valueId);
            }
        }
        // 根据平台属性值id集合查询对应的平台属性值
        List<PmsBaseAttrInfo> pmsBaseAttrInfos = pmsBaseAttrService.getAttrValueListByValueIds(attrValueIdsSet);

        // 制作面包屑
        // 获取已经选择的平台属性Id数组
        String[] valueIds = pmsSearchParam.getValueId();
        ArrayList<PmsSearchCrumb> pmsSearchCrumbs = new ArrayList<>();
/*        if (valueIds != null && valueIds.length > 0){
            for (String valueId : valueIds) {
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                pmsSearchCrumb.setUrlParam(getUrlParam(pmsSearchParam,valueId));
                Iterator<PmsBaseAttrInfo> pmsBaseAttrInfoIterator = pmsBaseAttrInfos.iterator();
                while (pmsBaseAttrInfoIterator.hasNext()){
                    List<PmsBaseAttrValue> attrValues = pmsBaseAttrInfoIterator.next().getAttrValueList();
                    for (PmsBaseAttrValue attrValue : attrValues) {
                        if (attrValue.getId().equals(valueId)){
                            pmsSearchCrumb.setValueName(attrValue.getValueName());
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
        }*/
        if (valueIds!=null && valueIds.length > 0){
            // 遍历每个平台属性值Id
            for (String valueId : valueIds) {
                // 制作面包屑
                PmsSearchCrumb pmsSearchCrumb = new PmsSearchCrumb();
                // 设置点击面包屑时的URL
                pmsSearchCrumb.setUrlParam(getUrlParam(pmsSearchParam,valueId));
                // 遍历全部商品的平台属性值集合
                Iterator<PmsBaseAttrInfo> pmsBaseAttrInfoIterator = pmsBaseAttrInfos.iterator();
                while (pmsBaseAttrInfoIterator.hasNext()){
                    // 获取对应的平台属性值集合
                    List<PmsBaseAttrValue> attrValues = pmsBaseAttrInfoIterator.next().getAttrValueList();
                    for (PmsBaseAttrValue attrValue : attrValues) {
                        // 判断平台属性值Id是否和已经选择的平台属性值Id相等
                        if (attrValue.getId().equals(valueId)){
                            // 设置面包屑的名称
                            pmsSearchCrumb.setValueName(attrValue.getValueName());
                            // 将当前的平台属性从集合中移除
                            pmsBaseAttrInfoIterator.remove();
                        }
                    }
                }
                pmsSearchCrumbs.add(pmsSearchCrumb);
            }
        }

        // 获取拼接平台属性的urlParam
        String currentUrl = getUrlParam(pmsSearchParam);
        modelMap.put("urlParam",currentUrl);
        modelMap.put("attrList",pmsBaseAttrInfos);
        modelMap.put("skuLsInfoList",pmsSearchSkuInfos);
        modelMap.put("attrValueSelectedList",pmsSearchCrumbs);

        return "list";
    }

    /**
     * 拼接点击平台属性值列表或点击面包屑时的URL
     * @param pmsSearchParam
     * @param valueIdForCrumb
     * @return 跳转的url
     */
    private String getUrlParam(PmsSearchParam pmsSearchParam,String... valueIdForCrumb) {

        String currentUrl = "";

        String catalog3Id = pmsSearchParam.getCatalog3Id();
        if (StringUtils.isNotBlank(catalog3Id)){
            if (StringUtils.isNotBlank(currentUrl)){
                currentUrl = currentUrl + "&";
            }
            currentUrl = currentUrl + "catalog3Id=" + catalog3Id;
        }

        String keyword = pmsSearchParam.getKeyword();
        if (StringUtils.isNotBlank(keyword)){
            if (StringUtils.isNotBlank(currentUrl)){
                currentUrl = currentUrl + "&";
            }
            currentUrl = currentUrl + "keyword=" + keyword;
        }

        String[] valueIds = pmsSearchParam.getValueId();
        if(valueIds!=null && valueIds.length > 0){
            for (String valueId : valueIds) {
             /*  如果面包屑的valueId为空则说明当前拼接的是点击平台属性列表时的URL
                 如果面包屑的valueId不为空,当前的传递的平台属性值Id不等于面包屑valueId,说明当前拼接的应该是点击平台属性列表时的URL
                 如果面包屑的valueId不为空,当前的传递的平台属性值Id等于面包屑valueId,说明当前拼接的应该是点击面包屑时拼接的URL
                     则不会进入下面方法,点击面包屑的valueId则不会被拼接,效果为(当前的URL-点击的面包屑valueId)*/
                if ((valueIdForCrumb == null || valueIdForCrumb.length == 0)
                        || (valueIdForCrumb!=null && valueIdForCrumb.length > 0 && !valueId.equals(valueIdForCrumb[0]))){
                    currentUrl = currentUrl + "&valueId=" + valueId;
                }
            }
        }
        return currentUrl;
    }

}
