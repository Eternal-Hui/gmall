package com.atguigu.gmall.order.Listeners;


import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OmsOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.math.BigDecimal;
import java.util.Date;

@Component
public class OrderMQListener {

    @Autowired
    OmsOrderService omsOrderService;

    @JmsListener(containerFactory = "jmsQueueListener",destination = "PAYMENT_SUCCESS_QUEUE")
    public void payment(MapMessage mapMessage) throws JMSException {

        String out_trade_no = mapMessage.getString("out_trade_no");
        String payAmount = mapMessage.getString("payAmount");

        // 更新订单信息
        OmsOrder omsOrder = new OmsOrder();

        omsOrder.setPayAmount(new BigDecimal(payAmount));
        omsOrder.setPaymentTime(new Date());
        omsOrder.setOrderSn(out_trade_no);
        omsOrder.setPayType(1);
        omsOrder.setStatus("1");

        // 发出执行锁定库存的消息队列
        omsOrderService.sendOrderResult(out_trade_no);

        // 执行更新订单操作
        omsOrderService.updateOrderByOrderSn(omsOrder);

    }
}
