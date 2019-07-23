package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OmsOrderItemMapper;
import com.atguigu.gmall.order.mapper.OmsorderMapper;
import com.atguigu.gmall.service.OmsOrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;
import java.util.UUID;
@Service
public class OmsOrderServiceImpl implements OmsOrderService {

    @Autowired
    OmsorderMapper omsorderMapper;

    @Autowired
    OmsOrderItemMapper omsOrderItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public String getTradeCode(String memberId) {

        String tradeCodeValue = UUID.randomUUID().toString();

        Jedis jedis = redisUtil.getJedis();
        try {
            jedis.setex("user:" + memberId + ":tradeCode",60*30,tradeCodeValue);
        } finally {
            jedis.close();
        }
        return tradeCodeValue;
    }

    @Override
    public boolean checkTradeCode(String memberId,String tradeCode) {

        Jedis jedis = redisUtil.getJedis();
        try {
            String tradeCodeFromCache = jedis.get("user:" + memberId + ":tradeCode");
            if (StringUtils.isNotBlank(tradeCodeFromCache)){
                if (tradeCodeFromCache.equals(tradeCode)){
                    // 删除交易码
                    jedis.del("user:" + memberId + ":tradeCode");
                    return true;
                }else {
                    return false;
                }
            }else {
                return false;
            }

        }finally {
            jedis.close();
        }

    }

    @Override
    public void addOrder(OmsOrder omsOrder) {
        omsorderMapper.insertSelective(omsOrder);
        String id = omsOrder.getId();
        List<OmsOrderItem> omsOrderItems = omsOrder.getOmsOrderItems();
        for (OmsOrderItem omsOrderItem : omsOrderItems) {
            omsOrderItem.setOrderId(id);
            omsOrderItemMapper.insertSelective(omsOrderItem);
        }
    }

    @Override
    public OmsOrder getOrderByOrderSn(String orderSn) {
        OmsOrder omsOrder = new OmsOrder();
        omsOrder.setOrderSn(orderSn);
        OmsOrder omsOrder1 = omsorderMapper.selectOne(omsOrder);
        OmsOrderItem omsOrderItem = new OmsOrderItem();
        omsOrderItem.setOrderId(omsOrder1.getId());
        List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItem);
        omsOrder1.setOmsOrderItems(omsOrderItems);
        return omsOrder1;
    }

    @Override
    public void updateOrderByOrderSn(OmsOrder omsOrder) {

        Example example = new Example(OmsOrder.class);
        example.createCriteria().andEqualTo("orderSn",omsOrder.getOrderSn());

        omsorderMapper.updateByExampleSelective(omsOrder,example);
    }

    @Override
    public void sendOrderResult(String out_trade_no) {

        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Session session = null;
        try {
            Connection connection = connectionFactory.createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();

            OmsOrder omsOrder = new OmsOrder();
            omsOrder.setOrderSn(out_trade_no);
            omsOrder = omsorderMapper.selectOne(omsOrder);

            OmsOrderItem omsOrderItem = new OmsOrderItem();
            omsOrderItem.setOrderId(omsOrder.getId());
            List<OmsOrderItem> omsOrderItems = omsOrderItemMapper.select(omsOrderItem);

            omsOrder.setOmsOrderItems(omsOrderItems);

            activeMQTextMessage.setText(JSON.toJSONString(omsOrder));

            Queue order_success_queue = session.createQueue("ORDER_SUCCESS_QUEUE");
            MessageProducer producer = session.createProducer(order_success_queue);
            producer.send(activeMQTextMessage);
            session.commit();
            producer.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }finally {
            try {
                session.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }


    }
}
