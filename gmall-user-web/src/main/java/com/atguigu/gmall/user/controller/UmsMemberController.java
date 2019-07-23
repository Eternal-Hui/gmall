package com.atguigu.gmall.user.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.service.UmsMemberService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


@Controller
public class UmsMemberController {

    @Reference
    UmsMemberService umsMemberService;

    @ResponseBody
    @RequestMapping("/get/all/user")
    public List<UmsMember> getAllUser( ){

        return umsMemberService.getAllUser();
    }

    @ResponseBody
    @RequestMapping("/get/user/by/id/{id}")
    public UmsMember getUserById(@PathVariable("id") String id){

        return umsMemberService.getUserById(id);
    }

}
