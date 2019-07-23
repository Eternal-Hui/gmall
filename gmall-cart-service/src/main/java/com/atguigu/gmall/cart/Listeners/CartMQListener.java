package com.atguigu.gmall.cart.Listeners;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.service.OmsCartItemService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.*;

@Component
public class CartMQListener {

    @Autowired
    OmsCartItemService omsCartItemService;


    @JmsListener(containerFactory = "jmsQueueListener",destination = "LOGIN_SUCCESS_QUEUE")
    public void payment(MapMessage mapMessage) throws JMSException {

        String memberId = mapMessage.getString("memberId");
        String omsCartItems = mapMessage.getString("omsCartItems");

        // Cookie中的购物车数据
        List<OmsCartItem> omsCartItemsFromCookie = JSON.parseArray(omsCartItems, OmsCartItem.class);

        // 根据memberId获取数据库中购物车数据
        List<OmsCartItem> omsCartItemsFromDb = omsCartItemService.getDbCartByMemberId(memberId);

        List<OmsCartItem> mergeOmsCartItem = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(omsCartItemsFromDb)){
            String nickName = omsCartItemsFromDb.get(0).getMemberNickname();

            Map<String,OmsCartItem> map = new HashMap<>();
            for (OmsCartItem omsCartItemFromDb : omsCartItemsFromDb) {
                map.put(omsCartItemFromDb.getProductSkuId(),omsCartItemFromDb);
                mergeOmsCartItem.add(omsCartItemFromDb);
            }

            for (OmsCartItem omsCartItemFromCache : omsCartItemsFromCookie) {
                OmsCartItem cartItemFromDb = map.get(omsCartItemFromCache.getProductSkuId());
                if (cartItemFromDb == null){
                    // 数据库购物车中没有该商品
                    omsCartItemFromCache.setMemberNickname(nickName);
                    omsCartItemFromCache.setMemberId(memberId);
                    omsCartItemFromCache.setCreateDate(new Date());
                    // 直接添加到合并的集合中
                    mergeOmsCartItem.add(omsCartItemFromCache);
                }else {
                    // 数据库中已经有该商品,更新数量
                    mergeOmsCartItem.remove(cartItemFromDb);
                    cartItemFromDb.setQuantity(cartItemFromDb.getQuantity().add(omsCartItemFromCache.getQuantity()));
                    cartItemFromDb.setModifyDate(new Date());
                    mergeOmsCartItem.add(cartItemFromDb);
                }
            }
        }else {
            // 数据库中购物车数据为空,则直接把缓存中的购物车数据存入数据库
            if (CollectionUtils.isNotEmpty(omsCartItemsFromCookie)){
                mergeOmsCartItem = omsCartItemsFromCookie;
            }
        }

        if (CollectionUtils.isNotEmpty(mergeOmsCartItem)){
            // 更新数据库购物车
            omsCartItemService.mergeCart(mergeOmsCartItem);
        }

    }

}
