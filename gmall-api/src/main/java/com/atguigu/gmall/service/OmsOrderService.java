package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

public interface OmsOrderService {
    String getTradeCode(String memberId);

    boolean checkTradeCode(String memberId,String tradeCode);

    void addOrder(OmsOrder omsOrder);

    OmsOrder getOrderByOrderSn(String orderSn);

    void updateOrderByOrderSn(OmsOrder omsOrder);

    void sendOrderResult(String out_trade_no);
}
