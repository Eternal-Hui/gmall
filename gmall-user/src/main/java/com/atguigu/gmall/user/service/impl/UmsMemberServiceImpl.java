package com.atguigu.gmall.user.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UmsMemberService;
import com.atguigu.gmall.user.mapper.UmsMemberMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class UmsMemberServiceImpl implements UmsMemberService {

    @Autowired
    private UmsMemberMapper umsMemberMapper;

    public List<UmsMember> getAllUser(){

        return umsMemberMapper.getAllUser();
    }

    public UmsMember getUserById(String memberId){

        UmsMember umsMember = new UmsMember();

        umsMember.setId(memberId);
        UmsMember one = umsMemberMapper.selectOne(umsMember);
        return one;
    }
}
