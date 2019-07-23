package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.OmsCartItemService;
import com.atguigu.gmall.service.OmsOrderService;
import com.atguigu.gmall.service.PmsSkuService;
import com.atguigu.gmall.service.UmsMemberService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class OrderController {

    @Reference
    OmsCartItemService omsCartItemService;

    @Reference
    UmsMemberService umsMemberService;

    @Reference
    OmsOrderService omsOrderService;

    @Reference
    PmsSkuService pmsSkuService;

    @LoginRequired(isNeedSuccess = true)
    @RequestMapping("submitOrder")
    public String submitOrder(String addressId, String tradeCode, HttpServletRequest request,ModelMap modelMap) {
        // 获取用户Id
        String memberId = (String) request.getAttribute("memberId");
        String nickname = (String) request.getAttribute("nickname");

        // 获取缓存中的购物车数据
        List<OmsCartItem> omsCartItems = omsCartItemService.getItemListCartCacheByMemberId(memberId);

        // 检查交易码是否有效,防止回退重复提交订单
        boolean boo = omsOrderService.checkTradeCode(memberId,tradeCode);
        if (boo){
            OmsOrder omsOrder = new OmsOrder();

            // 获取用户收货地址
            UmsMemberReceiveAddress receiveAddress = umsMemberService.getReceiveAddressByAddressId(addressId);
            // 生成外部订单号
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            String format = dateFormat.format(new Date());
            long currentTimeMillis = System.currentTimeMillis();
            String orderSn =  format + currentTimeMillis ;

            omsOrder.setOrderSn(orderSn);
            omsOrder.setCreateTime(new Date());
            omsOrder.setMemberId(memberId);
            omsOrder.setMemberUsername(nickname);
            omsOrder.setOrderType(0);
            omsOrder.setSourceType(0);
            BigDecimal totalAmount = getSumPrice(omsCartItems);
            omsOrder.setTotalAmount(totalAmount);
            omsOrder.setReceiverCity(receiveAddress.getCity());
            omsOrder.setReceiverDetailAddress(receiveAddress.getDetailAddress());
            omsOrder.setReceiverName(receiveAddress.getName());
            omsOrder.setReceiverPhone(receiveAddress.getPhoneNumber());
            omsOrder.setReceiverPostCode(receiveAddress.getPostCode());
            omsOrder.setReceiverProvince(receiveAddress.getProvince());
            omsOrder.setReceiverRegion(receiveAddress.getRegion());

            if (CollectionUtils.isNotEmpty(omsCartItems)) {

                List<OmsOrderItem> omsOrderItems = new ArrayList<>();
                for (OmsCartItem omsCartItem : omsCartItems) {
                    if (omsCartItem.getIsChecked().equals("1")){
                        OmsOrderItem omsOrderItem = new OmsOrderItem();

                        String productSkuId = omsCartItem.getProductSkuId();
                        PmsSkuInfo pmsSkuInfo = pmsSkuService.getSkuInfoById(productSkuId);

                        int i = omsCartItem.getPrice().compareTo(pmsSkuInfo.getPrice());
                        // 价格没变
                        if (i == 0){
                            BeanUtils.copyProperties(omsCartItem,omsOrderItem);
                            omsOrderItem.setOrderSn(orderSn);
                            omsOrderItem.setProductPrice(omsCartItem.getPrice());
                            omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                            omsOrderItem.setRealAmount(omsOrderItem.getProductPrice().multiply(omsOrderItem.getProductQuantity()));
                        }else {
                            // 价格发生变化
                            modelMap.put("errMsg","订单商品价格已发生变化");
                            return "tradeFail";
                        }
                        omsOrderItems.add(omsOrderItem);
                    }
                }

                omsOrder.setOmsOrderItems(omsOrderItems);
                // 保存订单到数据库
                omsOrderService.addOrder(omsOrder);
                // 删除购物车中对应的商品
                omsCartItemService.delOrderItem(omsCartItems);

                return "redirect:http://payment.gmall.com:8090/paymentIndex?orderSn=" + orderSn + "&totalAmount=" + totalAmount;
            }else {
                modelMap.put("errMsg","购物车商品可能不存在,请重新添加");
                return "tradeFail";
            }

        }else {
            modelMap.put("errMsg","不能多次提交同一订单");
            return "tradeFail";
        }
    }

    @LoginRequired(isNeedSuccess = true)
    @RequestMapping("toTrade")
    public String toTrade(ModelMap modelMap, HttpServletRequest request) {
        String memberId = (String) request.getAttribute("memberId");
        String nickName = (String) request.getAttribute("nickname");

        // 从缓存中获取购物车商品列表
        List<OmsCartItem> omsCartItems = omsCartItemService.getItemListCartCacheByMemberId(memberId);

        List<OmsOrderItem> omsOrderItems = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(omsCartItems)) {
            // 遍历购物车集合,将购物车对象转化为订单对象
            for (OmsCartItem omsCartItem : omsCartItems) {
                if (omsCartItem.getIsChecked().equals("1")) {
                    OmsOrderItem omsOrderItem = new OmsOrderItem();
                    omsOrderItem.setProductName(omsCartItem.getProductName());
                    omsOrderItem.setProductCategoryId(omsCartItem.getProductCategoryId());
                    omsOrderItem.setProductPic(omsCartItem.getProductPic());
                    omsOrderItem.setProductPrice(omsCartItem.getPrice());
                    omsOrderItem.setProductQuantity(omsCartItem.getQuantity());
                    omsOrderItem.setRealAmount(omsOrderItem.getProductPrice().multiply(omsOrderItem.getProductQuantity()));
                    omsOrderItem.setProductId(omsCartItem.getProductId());
                    omsOrderItem.setProductAttr(omsCartItem.getProductAttr());
                    omsOrderItem.setProductSkuId(omsCartItem.getProductSkuId());
                    omsOrderItem.setProductSkuCode(omsCartItem.getProductSkuCode());

                    omsOrderItems.add(omsOrderItem);
                }
            }
        }

        // 生成交易码tradeCode
        String tradeCode = omsOrderService.getTradeCode(memberId);

        // 根据member获取收货地址表
        List<UmsMemberReceiveAddress> umsMemberReceiveAddresses = umsMemberService.getReceiveAddressByMemberId(memberId);

        BigDecimal totalAmount = getSumPrice(omsCartItems);
        modelMap.put("orderDetailList", omsOrderItems);
        modelMap.put("nickName", nickName);
        modelMap.put("totalAmount", totalAmount);
        modelMap.put("userAddressList", umsMemberReceiveAddresses);
        modelMap.put("tradeCode", tradeCode);
        return "trade";
    }

    private BigDecimal getSumPrice(List<OmsCartItem> omsCartItems) {
        BigDecimal sumPrice = new BigDecimal("0.00");

        if (CollectionUtils.isNotEmpty(omsCartItems)) {
            for (OmsCartItem omsCartItem : omsCartItems) {
                if ("1".equals(omsCartItem.getIsChecked())) {
                    sumPrice = sumPrice.add(omsCartItem.getTotalPrice());
                }
            }
        }
        return sumPrice;
    }

}
