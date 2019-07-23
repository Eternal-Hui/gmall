package com.atguigu.gmall.manage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ManageController {

    @ResponseBody
    @RequestMapping("index")
    public String getIndex(){
        return "index";
    }
}
