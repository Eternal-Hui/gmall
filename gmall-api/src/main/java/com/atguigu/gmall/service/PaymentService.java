package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {
    void addPaymentInfo(PaymentInfo paymentInfo);

    void sendPaymentResult(PaymentInfo paymentInfo);

    void updatePaymentInfoByOrderSn(PaymentInfo paymentInfo);

    void sendCheckPayStatusQueue(String orderSn,int count);

    String getDbpayStatus(String out_trade_no);

    Map<String, Object> checkAlipayStatus(String out_trade_no);
}
