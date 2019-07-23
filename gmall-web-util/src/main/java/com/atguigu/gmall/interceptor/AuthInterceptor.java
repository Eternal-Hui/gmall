package com.atguigu.gmall.interceptor;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        HandlerMethod handlerMethod = (HandlerMethod)handler;

        LoginRequired methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequired.class);

        if (methodAnnotation == null){
            return true;
        }

        String token = "";

        // 从cookie中获取token
        String oldToken = CookieUtil.getCookieValue(request,"oldToken",true);
        if (StringUtils.isNotBlank(oldToken)){
            token = oldToken;
        }
        // 从请求地址中获取token
        String newToken = request.getParameter("newToken");
        if (StringUtils.isNotBlank(newToken)){
            token = newToken;
        }

        // 获取业务请求地址
        String returnUrl = request.getRequestURL().toString();

        if (StringUtils.isNotBlank(token)){

            // 发送http请求验证token的有效性
            String result = HttpclientUtil.doGet("http://passport.gmall.com:8085/verify?token=" + token);
            Map resultMap = JSON.parseObject(result, Map.class);
            String isSuccess = (String) resultMap.get("result");

            if ("success".equals(isSuccess)){
                request.setAttribute("memberId",resultMap.get("memberId"));
                request.setAttribute("nickname",resultMap.get("nickname"));
                // 刷新cookie
                CookieUtil.setCookie(request,response,"oldToken",token,60*30,true);
            }else {
                // token无效,重定向到登录页面
                response.sendRedirect("http://passport.gmall.com:8085/loginIndex?returnUrl=" + returnUrl);
                return false;
            }
        }else {
            // token为空,判断是否一定要登录成功
            if (methodAnnotation.isNeedSuccess()){
                response.sendRedirect("http://passport.gmall.com:8085/loginIndex?returnUrl=" + returnUrl);
                return false;
            }
        }

        return true;
    }

}
