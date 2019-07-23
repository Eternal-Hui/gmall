package com.atguigu.gmall.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void addPaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo) {

        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_success_queue = session.createQueue("PAYMENT_SUCCESS_QUEUE");

            MessageProducer producer = session.createProducer(payment_success_queue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no",paymentInfo.getOrderSn());
            mapMessage.setString("payAmount",String.valueOf(paymentInfo.getTotalAmount()));
            mapMessage.setString("status","success");

            producer.send(mapMessage);

            session.commit();
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void updatePaymentInfoByOrderSn(PaymentInfo paymentInfo) {

        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderSn",paymentInfo.getOrderSn());

        paymentInfoMapper.updateByExampleSelective(paymentInfo,example);
    }

    @Override
    public void sendCheckPayStatusQueue(String orderSn, int count) {

        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = null;
        try {
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_success_queue = session.createQueue("PAYMENT_CHECK_QUEUE");

            MessageProducer producer = session.createProducer(payment_success_queue);
            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no",orderSn);
            mapMessage.setInt("count",count);
            // 开启消息队列延迟一分钟发送
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,60*1000*2);

            producer.send(mapMessage);

            session.commit();

        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getDbpayStatus(String out_trade_no) {

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderSn(out_trade_no);

        PaymentInfo paymentInfo1 = paymentInfoMapper.selectOne(paymentInfo);

        if (StringUtils.isNotBlank(paymentInfo1.getPaymentStatus()) && paymentInfo1.getPaymentStatus().equals("已支付")){
            return "success";
        }else {
            return "fail";
        }

    }

    @Override
    public Map<String, Object> checkAlipayStatus(String out_trade_no) {

        Map<String,Object> returnMap = new HashMap<>();

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        Map<String,String> map = new HashMap<>();
        map.put("out_trade_no",out_trade_no);
        String jsonString = JSON.toJSONString(map);
        request.setBizContent(jsonString);
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            String tradeStatus = response.getTradeStatus();
            returnMap.put("tradeStatus",tradeStatus);
            returnMap.put("out_trade_no",response.getOutTradeNo());
            returnMap.put("tradeNo",response.getTradeNo());
            String callbackContent = JSON.toJSONString(response);
            returnMap.put("callbackContent",callbackContent);

        } else {
            // 用户还未登录支付宝
            returnMap.put("tradeStatus","");
            returnMap.put("out_trade_no",out_trade_no);
        }

        return returnMap;
    }
}
