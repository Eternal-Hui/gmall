package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.cart.mapper.OmsCartItemMapper;
import com.atguigu.gmall.service.OmsCartItemService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service
public class OmsCartItemServiceImpl implements OmsCartItemService {

    @Autowired
    OmsCartItemMapper omsCartItemMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public OmsCartItem getItemExist(OmsCartItem omsCartItem) {

        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setMemberId(omsCartItem.getMemberId());
        cartItem.setProductSkuId(omsCartItem.getProductSkuId());

        return omsCartItemMapper.selectOne(cartItem);
    }

    @Override
    public void addToCartListDb(OmsCartItem omsCartItem) {
        omsCartItemMapper.insertSelective(omsCartItem);
        // 刷新Redis缓存
        flushCache(omsCartItem.getMemberId());
    }

    @Override
    public void updateCartListDb(OmsCartItem omsCartItemFromDb) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setModifyDate(omsCartItemFromDb.getModifyDate());
        omsCartItem.setQuantity(omsCartItemFromDb.getQuantity());
        omsCartItem.setPrice(omsCartItemFromDb.getPrice());

        Example example = new Example(OmsCartItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("id",omsCartItemFromDb.getId());

        omsCartItemMapper.updateByExampleSelective(omsCartItem,example);
        // 刷新Redis缓存
        flushCache(omsCartItemFromDb.getMemberId());
    }

    @Override
    public List<OmsCartItem> getItemListCartCacheByMemberId(String memberId) {
        List<OmsCartItem> omsCartItems = new ArrayList<>();
        Jedis jedis =null;
        try {
            jedis = redisUtil.getJedis();
            List<String> omsCartItemsStr = jedis.hvals("user:" + memberId + ":cart");

            if (CollectionUtils.isNotEmpty(omsCartItemsStr)){

                for (String omsCartItemStr : omsCartItemsStr) {
                    OmsCartItem omsCartItem = JSON.parseObject(omsCartItemStr, OmsCartItem.class);
                    omsCartItem.setTotalPrice(omsCartItem.getPrice().multiply(omsCartItem.getQuantity()));
                    omsCartItems.add(omsCartItem);
                }
            }
        }finally {
            jedis.close();
        }

        return omsCartItems;
    }

    @Override
    public void updateCartDbChecked(OmsCartItem omsCartItem) {

        OmsCartItem cartItem = new OmsCartItem();
        cartItem.setProductSkuId(omsCartItem.getProductSkuId());
        cartItem.setIsChecked(omsCartItem.getIsChecked());

        Example example = new Example(OmsCartItem.class);
        example.createCriteria().andEqualTo("productSkuId",omsCartItem.getProductSkuId())
                                .andEqualTo("memberId",omsCartItem.getMemberId());

        omsCartItemMapper.updateByExampleSelective(cartItem,example);

        // 同步缓存
        flushCache(omsCartItem.getMemberId());
    }

    @Override
    public void delOrderItem(List<OmsCartItem> omsCartItems) {
        String memberId = omsCartItems.get(0).getMemberId();
        for (OmsCartItem omsCartItem : omsCartItems) {
            if (omsCartItem.getIsChecked().equals("1")){
                Example example = new Example(OmsCartItem.class);
                example.createCriteria().andEqualTo("id",omsCartItem.getId());
                omsCartItemMapper.deleteByExample(example);
            }
        }
        // 同步缓存
        flushCache(memberId);

    }

    @Override
    public List<OmsCartItem> getDbCartByMemberId(String memberId) {

        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);

        List<OmsCartItem> omsCartItems = omsCartItemMapper.select(omsCartItem);

        return omsCartItems;
    }

    @Override
    public void mergeCart(List<OmsCartItem> omsCartItemsFromDb) {

        // 先把原来的购物车数据删除
        String memberId = omsCartItemsFromDb.get(0).getMemberId();
        OmsCartItem omsCartItem = new OmsCartItem();
        omsCartItem.setMemberId(memberId);

        omsCartItemMapper.delete(omsCartItem);

        for (OmsCartItem cartItem : omsCartItemsFromDb) {
            omsCartItemMapper.insertSelective(cartItem);
        }

        // 同步缓存
        flushCache(memberId);
    }

    private void flushCache(String memberId){
        Jedis jedis =null;
        try {
            jedis = redisUtil.getJedis();

            OmsCartItem cartItem = new OmsCartItem();
            cartItem.setMemberId(memberId);
            List<OmsCartItem> omsCartItems = omsCartItemMapper.select(cartItem);

            String redisKey = "user:" + memberId + ":cart";
            if (CollectionUtils.isNotEmpty(omsCartItems)){
                jedis.del(redisKey);
                Map<String, String> redisMap = new HashMap<>();
                for (OmsCartItem omsCartItem : omsCartItems) {
                    String omsCartItemJSONStr = JSON.toJSONString(omsCartItem);
                    redisMap.put(omsCartItem.getProductSkuId(),omsCartItemJSONStr);
                }
                jedis.hmset(redisKey,redisMap);
            }
        } finally {
            jedis.close();
        }
    }

}
