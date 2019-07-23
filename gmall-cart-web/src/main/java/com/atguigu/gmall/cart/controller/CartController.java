package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.PmsSkuInfo;
import com.atguigu.gmall.service.OmsCartItemService;
import com.atguigu.gmall.service.PmsSkuService;
import com.atguigu.gmall.util.CookieUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class CartController {

    @Reference
    PmsSkuService pmsSkuService;

    @Reference
    OmsCartItemService omsCartItemService;

    @LoginRequired
    @RequestMapping("checkCart")
    public String checkCart(OmsCartItem omsCartItem,HttpServletRequest request, HttpServletResponse response,ModelMap modelMap){
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        // 判断用户是否已登录
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        if (StringUtils.isBlank(memberId)){
            // 用户未登录,从cookie中获取数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)){
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                for (OmsCartItem cartItem : omsCartItems) {
                    // 遍历购物车所有商品,找到与客户端传过来相同的skuId,并修改选中状态
                    if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                        cartItem.setIsChecked(omsCartItem.getIsChecked());
                    }
                }
                // 重新覆盖更新cookie
                String omsCartItemsJSONStr = JSON.toJSONString(omsCartItems);
                CookieUtil.setCookie(request,response,"cartListCookie",omsCartItemsJSONStr,1000*60,true);
            }

        }else {
            // 用户已登录,更新Db中数据
            omsCartItem.setMemberId(memberId);
            omsCartItemService.updateCartDbChecked(omsCartItem);
            // 从缓存中获取数据返回
            omsCartItems = omsCartItemService.getItemListCartCacheByMemberId(memberId);
        }
        BigDecimal sumPrice = getSumPrice(omsCartItems);
        modelMap.put("cartList",omsCartItems);
        modelMap.put("sumPrice",sumPrice);

        return "cartListInner";
    }

    @LoginRequired
    @RequestMapping("cartList")
    public String cartList(HttpServletRequest request,ModelMap modelMap){

        List<OmsCartItem> omsCartItems = new ArrayList<>();

        // 判断用户是否已登录
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        if (StringUtils.isBlank(memberId)){// 用户未登录
            // 从cookie中取出购物车数据
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isNotBlank(cartListCookie)){
                // 获取cookie中购物车商品集合
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            }
        }else{// 用户已登录
            // 从缓存中取出购物车数据
            omsCartItems = omsCartItemService.getItemListCartCacheByMemberId(memberId);
        }

        BigDecimal sumPrice = getSumPrice(omsCartItems);

        modelMap.put("cartList",omsCartItems);
        modelMap.put("sumPrice",sumPrice);

        return "cartList";
    }

    private BigDecimal getSumPrice(List<OmsCartItem> omsCartItems) {
        BigDecimal sumPrice = new BigDecimal("0.00");

        if (CollectionUtils.isNotEmpty(omsCartItems)){
            for (OmsCartItem omsCartItem : omsCartItems) {
                if ("1".equals(omsCartItem.getIsChecked())){
                    sumPrice = sumPrice.add(omsCartItem.getTotalPrice());
                }
            }
        }
        return sumPrice;
    }

    @LoginRequired
    @RequestMapping("addToCart")
    public String addToCart(OmsCartItem omsCartItem, HttpServletRequest request, HttpServletResponse response, ModelMap modelMap){

        // 创建一个集合用于存放每次添加购物车的商品
        List<OmsCartItem> omsCartItems = new ArrayList<>();

        // 根据skuId获取对应的商品信息
        PmsSkuInfo pmsSkuInfo = pmsSkuService.getSkuInfoById(omsCartItem.getProductSkuId());

        // 设置添加的购物车商品信息
        omsCartItem.setCreateDate(new Date());
        omsCartItem.setIsChecked("1");
        omsCartItem.setPrice(pmsSkuInfo.getPrice());
        omsCartItem.setProductCategoryId(pmsSkuInfo.getCatalog3Id());
        omsCartItem.setProductId(pmsSkuInfo.getProductId());
        omsCartItem.setProductName(pmsSkuInfo.getSkuName());
        omsCartItem.setProductPic(pmsSkuInfo.getSkuDefaultImg());
        omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));

        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");
        // 判断用户是否已经登录
        if (StringUtils.isBlank(memberId)){
            // 用户未登录,获取浏览器中的cookie,检查cookie是否存在
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            if (StringUtils.isBlank(cartListCookie)){
                // cookie为空,直接添加到购物车
                omsCartItems.add(omsCartItem);
            }else{
                // cookie不为空,取出cookie中的商品集合
                omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
                // 判断当前商品是否已经在购物车
                boolean boo = isNewInCart(omsCartItems,omsCartItem);
                if (boo){
                    // 当前添加的商品未在购物车中,新增商品到购物车
                    omsCartItems.add(omsCartItem);
                }else{
                    // 当前商品已经在购物车中,更新数量和总价
                    for (OmsCartItem cartItem : omsCartItems) {
                        // 遍历cookie中商品,找到和当前添加的相同商品
                        if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                            // 更新数量
                            cartItem.setQuantity(cartItem.getQuantity().add(omsCartItem.getQuantity()));
                            // 更新总价
                            cartItem.setTotalPrice(cartItem.getPrice().multiply(cartItem.getQuantity()));
                            // 结束遍历
                            break;
                        }
                    }
                }
            }
            String omsCartItemsJSONStr = JSON.toJSONString(omsCartItems);
            System.out.println(omsCartItemsJSONStr);
            CookieUtil.setCookie(request,response,"cartListCookie",omsCartItemsJSONStr,60*60*10,true);

        }else{
            // 用户已经登录,根据memberId和skuId到数据库查询该用户购物车中是否已经存在该商品
            omsCartItem.setMemberId(memberId);
            omsCartItem.setMemberNickname(nickname);

            OmsCartItem omsCartItemFromDb = omsCartItemService.getItemExist(omsCartItem);

            if (omsCartItemFromDb == null){
                // null,该用户购物车尚未有该商品,添加商品到购物车中
                String productId = omsCartItem.getProductId();
                if (productId.length() < 3){
                    productId = "0" + productId;
                }
                String skuId = omsCartItem.getProductSkuId();
                if (skuId.length() < 3){
                    skuId = "0" + skuId;
                }
                String skuCode = "2019" + productId + skuId;
                omsCartItem.setProductSkuCode(skuCode);
                omsCartItemService.addToCartListDb(omsCartItem);
            }else {
                // 不为null,说明该用户购物车已经有该商品,更新数量和单价
                omsCartItemFromDb.setModifyDate(new Date());
                omsCartItemFromDb.setQuantity(omsCartItemFromDb.getQuantity().add(omsCartItem.getQuantity()));
                omsCartItemFromDb.setPrice(omsCartItem.getPrice());
                omsCartItemService.updateCartListDb(omsCartItemFromDb);
            }
        }

        BigDecimal num = omsCartItem.getQuantity();
        String skuId = omsCartItem.getProductSkuId();
        return "redirect:/success?skuId=" + skuId + "&num=" + num;
    }

    @LoginRequired
    @RequestMapping("success")
    public String success(@RequestParam("skuId") String skuId,@RequestParam("num") BigDecimal num,ModelMap modelMap){

        PmsSkuInfo pmsSkuInfo = pmsSkuService.getSkuInfoById(skuId);

        modelMap.put("skuInfo",pmsSkuInfo);
        modelMap.put("skuNum",num);
        return "success";
    }

    private boolean isNewInCart(List<OmsCartItem> omsCartItems, OmsCartItem omsCartItem) {

        boolean boo = true;

        for (OmsCartItem cartItem : omsCartItems) {
            if (cartItem.getProductSkuId().equals(omsCartItem.getProductSkuId())){
                boo = false;
                break;
            }
        }
        return boo;
    }

}
