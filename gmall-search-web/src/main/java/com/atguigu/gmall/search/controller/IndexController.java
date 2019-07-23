package com.atguigu.gmall.search.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.service.OmsCartItemService;
import com.atguigu.gmall.util.CookieUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class IndexController {

    @Reference
    OmsCartItemService omsCartItemService;

    @LoginRequired
    @RequestMapping("index")
    public String index(HttpServletRequest request, ModelMap modelMap) {

        String memberId = (String) request.getAttribute("memberId");

        List<OmsCartItem> omsCartItems = omsCartItemService.getItemListCartCacheByMemberId(memberId);
        int count = omsCartItems.size();
        modelMap.put("count", count);

        return "index";
    }

    @RequestMapping("logout")
    @ResponseBody
    public String logout(String memberId, HttpServletRequest request, HttpServletResponse response) {

        // 将cookie删除
        CookieUtil.deleteCookie(request, response, "oldToken");
        request.removeAttribute("nickname");
        request.removeAttribute("memberId");

        return "result";
    }

}
