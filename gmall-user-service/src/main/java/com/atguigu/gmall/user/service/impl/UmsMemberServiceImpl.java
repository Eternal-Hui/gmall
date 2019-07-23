package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;
import com.atguigu.gmall.service.UmsMemberService;
import com.atguigu.gmall.user.mapper.UmsMemberMapper;
import com.atguigu.gmall.user.mapper.UmsMemberReceiveAddressMapper;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.List;

@Service
public class UmsMemberServiceImpl implements UmsMemberService {

    @Autowired
    private UmsMemberMapper umsMemberMapper;

    @Autowired
    private UmsMemberReceiveAddressMapper umsMemberReceiveAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    ActiveMQUtil activeMQUtil;


    @Override
    public List<UmsMember> getAllUser(){

        return umsMemberMapper.selectAll();
       // return umsMemberMapper.getAllUser();
    }

    @Override
    public UmsMember getUserById(String memberId){

        UmsMember umsMember = new UmsMember();

        umsMember.setId(memberId);
        UmsMember one = umsMemberMapper.selectOne(umsMember);
        return one;
    }

    @Override
    public UmsMember login(UmsMember umsMember) {

        UmsMember umsMember1 = new UmsMember();
        umsMember1.setUsername(umsMember.getUsername());
        umsMember1.setPassword(umsMember.getPassword());

        UmsMember umsMemberFromDb = umsMemberMapper.selectOne(umsMember1);

        if (umsMemberFromDb != null){
            // 同步缓存
            Jedis jedis = redisUtil.getJedis();
            try {
                jedis.setex("user:" + umsMemberFromDb.getId() + ":info",60*60, JSON.toJSONString(umsMemberFromDb));
            } finally {
                jedis.close();
            }

        }

        return  umsMemberFromDb;
    }

    @Override
    public List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId) {

        UmsMemberReceiveAddress umsMemberReceiveAddress = new UmsMemberReceiveAddress();
        umsMemberReceiveAddress.setMemberId(memberId);

        return umsMemberReceiveAddressMapper.select(umsMemberReceiveAddress);
    }

    @Override
    public UmsMember addOauthUser(UmsMember umsMember) {

        UmsMember returnMember = new UmsMember();
        // 判断数据库中是否已经存在该用户
        UmsMember umsMember1 = new UmsMember();
        umsMember1.setSourceUid(umsMember.getSourceUid());

        UmsMember one = umsMemberMapper.selectOne(umsMember1);
        if (one == null){
            umsMemberMapper.insertSelective(umsMember);
            returnMember = umsMember;
        }else {
            // 更新code和access_token
            Example example = new Example(UmsMember.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("accessCode",umsMember.getAccessCode()).andEqualTo("accessToken",umsMember.getAccessToken());
            umsMemberMapper.updateByExampleSelective(umsMember1,example);
            returnMember = one;
        }
        return returnMember;
    }

    @Override
    public void putTokenIntoCache(String token, String memberId) {

        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            jedis.setex("user:" + memberId + ":token",60*30,token);
        } finally {
            jedis.close();
        }

    }

    @Override
    public UmsMemberReceiveAddress getReceiveAddressByAddressId(String addressId) {

        UmsMemberReceiveAddress receiveAddress = new UmsMemberReceiveAddress();
        receiveAddress.setId(addressId);

        return umsMemberReceiveAddressMapper.selectOne(receiveAddress);
    }

    @Override
    public void sendMergeCartQueue(String id,List<OmsCartItem> omsCartItems) {

        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Session session = null;
        try {
            Connection connection = connectionFactory.createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);

            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("memberId",id);
            activeMQMapMessage.setString("omsCartItems",JSON.toJSONString(omsCartItems));

            Queue login_success_queue = session.createQueue("LOGIN_SUCCESS_QUEUE");
            MessageProducer producer = session.createProducer(login_success_queue);
            producer.send(activeMQMapMessage);

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
