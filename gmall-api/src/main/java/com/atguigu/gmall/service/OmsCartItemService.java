package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;

import java.util.List;

public interface OmsCartItemService {
    OmsCartItem getItemExist(OmsCartItem omsCartItem);

    void addToCartListDb(OmsCartItem omsCartItem);

    void updateCartListDb(OmsCartItem omsCartItemFromDb);

    List<OmsCartItem> getItemListCartCacheByMemberId(String memberId);

    void updateCartDbChecked(OmsCartItem omsCartItem);

    void delOrderItem(List<OmsCartItem> omsCartItems);

    List<OmsCartItem> getDbCartByMemberId(String memberId);

    void mergeCart(List<OmsCartItem> omsCartItemsFromDb);
}
