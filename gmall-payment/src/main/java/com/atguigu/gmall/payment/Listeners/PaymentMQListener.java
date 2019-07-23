package com.atguigu.gmall.payment.Listeners;


import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Map;

@Component
public class PaymentMQListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(containerFactory = "jmsQueueListener", destination = "PAYMENT_CHECK_QUEUE")
    public void paymentStatus(MapMessage mapMessage) throws JMSException {

        String out_trade_no = mapMessage.getString("out_trade_no");
        int count = mapMessage.getInt("count");
        // 查询支付宝订单支付状态
        Map<String, Object> payStatusMap = paymentService.checkAlipayStatus(out_trade_no);
        count--;
        String tradeStatus = (String) payStatusMap.get("tradeStatus");

        if (StringUtils.isNotBlank(tradeStatus)) {
            if (tradeStatus.equals("WAIT_BUYER_PAY")) {
                // 支付订单已创建,等待用户支付
                if (count > 0) {
                    // 重新发检查支付状态的消息队列
                    paymentService.sendCheckPayStatusQueue(out_trade_no, count);
                } else {
                    System.out.println("检查次数耗尽,结束检查");
                }
            }

            // 检查交易幂等性,数据库中的订单支付状态
            String dbpayStatus = paymentService.getDbpayStatus(out_trade_no);
            if (!dbpayStatus.equals("success")) {
                // 更新支付信息
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOrderSn(out_trade_no);
                if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
                    paymentInfo.setPaymentStatus("已支付");
                } else {
                    paymentInfo.setPaymentStatus("交易已关闭");
                }
                String callbackContent = (String) payStatusMap.get("callbackContent");
                paymentInfo.setCallbackContent(callbackContent);
                String tradeNo = (String) payStatusMap.get("tradeNo");
                paymentInfo.setAlipayTradeNo(tradeNo);

                paymentService.updatePaymentInfoByOrderSn(paymentInfo);
                // 发出更新订单信息的消息队列
                paymentService.sendPaymentResult(paymentInfo);

            }
        } else {
            // 用户还未登录支付宝支付
            if (count > 0) {
                // 重新发检查支付状态的消息队列
                paymentService.sendCheckPayStatusQueue(out_trade_no, count);
            } else {
                System.out.println("检查次数5次耗尽");
            }
        }

    }
}
