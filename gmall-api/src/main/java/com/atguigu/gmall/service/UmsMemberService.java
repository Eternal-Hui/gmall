package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.bean.UmsMemberReceiveAddress;

import java.util.List;

public interface UmsMemberService {

    List<UmsMember> getAllUser();

    UmsMember getUserById(String memberId);

    UmsMember login(UmsMember umsMember);

    List<UmsMemberReceiveAddress> getReceiveAddressByMemberId(String memberId);

    UmsMember addOauthUser(UmsMember umsMember);

    void putTokenIntoCache(String token, String id);

    UmsMemberReceiveAddress getReceiveAddressByAddressId(String addressId);

    void sendMergeCartQueue(String id,List<OmsCartItem> omsCartItems);
}
