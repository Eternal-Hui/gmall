package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsCartItem;
import com.atguigu.gmall.bean.UmsMember;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UmsMemberService;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UmsMemberService umsMemberService;


    @RequestMapping("weiboLogin")
    public String weiboLogin(String code,HttpServletRequest request){

        String appId = "2186719078";
        String secretId = "0360b59c7fc725d40632f355703a9aac";

        // 回调地址
        String url1 = "http://passport.gmall.com:8085/weiboLogin";

        //授权码请求地址
        String url2 = "https://api.weibo.com/oauth2/access_token";

        Map<String,String> map = new HashMap<>();
        map.put("client_id",appId);
        map.put("client_secret",secretId);
        map.put("grant_type","authorization_code");
        map.put("redirect_uri",url1);
        map.put("code",code);

        String token = null;

        String jsonStr = HttpclientUtil.doPost(url2, map);
        if (StringUtils.isNotBlank(jsonStr)){
            HashMap hashMap = JSON.parseObject(jsonStr, new HashMap<String, String>().getClass());
            String access_Token = (String) hashMap.get("access_token");
            String uid = (String) hashMap.get("uid");

            // 根据微博用户id获取用户信息的地址
            String url3 = "https://api.weibo.com/2/users/show.json?access_token=" + access_Token + "&uid=" + uid;
            // 获取到的微博用户信息json字符串
            String weiboUserInfoJsonStr = HttpclientUtil.doGet(url3);
            HashMap weiboUserMap = JSON.parseObject(weiboUserInfoJsonStr, new HashMap<String, String>().getClass());

            // 获取用户相关信息
            String nickname = (String) weiboUserMap.get("screen_name");
            String username = (String) weiboUserMap.get("name");
            String gender = (String) weiboUserMap.get("gender");
            String city = (String) weiboUserMap.get("location");
            String personalizedSignature = (String) weiboUserMap.get("description");
            String icon = (String) weiboUserMap.get("profile_image_url");

            UmsMember umsMember = new UmsMember();
            umsMember.setUsername(username);
            umsMember.setCity(city);
            umsMember.setNickname(nickname);
            umsMember.setSourceType(2);
            umsMember.setSourceUid(Long.parseLong(uid));
            umsMember.setPersonalizedSignature(personalizedSignature);
            umsMember.setIcon(icon);
            umsMember.setCreateTime(new Date());
            umsMember.setAccessCode(code);
            umsMember.setAccessToken(access_Token);
            int genderI = 0;
            if (gender.equals("m")){
                genderI = 1;
            }
            if(gender.equals("f")) {
                genderI = 2;
            }
            umsMember.setGender(genderI);
            // 保存微博用户信息到数据库
            UmsMember umsMemberFromDb = umsMemberService.addOauthUser(umsMember);

            // 生成token
            String salt;
            String remoteAddr = request.getRemoteAddr();
            if (StringUtils.isBlank(remoteAddr)){
                salt = "127.0.0.1";
            }else {
                salt = remoteAddr;
            }

            Map<String,Object> tokenMap = new HashMap<>();
            tokenMap.put("nickname",umsMemberFromDb.getNickname());
            tokenMap.put("memberId",umsMemberFromDb.getId());

            token = JwtUtil.encode("gmall", tokenMap, salt);
            // token存入缓存
            umsMemberService.putTokenIntoCache(token,umsMemberFromDb.getId());
            // 发出合并购物车消息队列通知
            String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
            List<OmsCartItem> omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
            umsMemberService.sendMergeCartQueue(umsMemberFromDb.getId(),omsCartItems);
        }

        // 携带token重定向回到首页
        return "redirect:http://search.gmall.com:8083/index?newToken=" + token;
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UmsMember umsMember,HttpServletRequest request){

        String token = "";

        if (StringUtils.isBlank(umsMember.getUsername()) || StringUtils.isBlank(umsMember.getPassword())){
            return null;
        }
        UmsMember umsMemberFormDb = umsMemberService.login(umsMember);

        if (umsMemberFormDb!= null){
            // 说明登录成功
            String salt;
            String remoteAddr = request.getRemoteAddr();
            if (StringUtils.isBlank(remoteAddr)){
                salt = "127.0.0.1";
            }else {
                salt = remoteAddr;
            }

            Map<String,Object> map = new HashMap<>();
            map.put("nickname",umsMemberFormDb.getNickname());
            map.put("memberId",umsMemberFormDb.getId());

            token = JwtUtil.encode("gmall", map, salt);
        }
        // 把token存入缓存
        umsMemberService.putTokenIntoCache(token,umsMemberFormDb.getId());

        // 发出合并购物车消息队列通知
        String cartListCookie = CookieUtil.getCookieValue(request, "cartListCookie", true);
        List<OmsCartItem> omsCartItems = JSON.parseArray(cartListCookie, OmsCartItem.class);
        umsMemberService.sendMergeCartQueue(umsMemberFormDb.getId(),omsCartItems);

        return token;
    }

    @RequestMapping("loginIndex")
    public String loginIndex(String returnUrl, ModelMap modelMap){

        modelMap.put("returnUrl",returnUrl);

        return "loginIndex";
    }

    @RequestMapping("verify")
    @ResponseBody
    public Map<String,String> verify(String token, HttpServletRequest request){

        Map<String,String> returnMap = new HashMap<>();

        if (StringUtils.isBlank(token)){
            returnMap.put("result","failed");
            return returnMap;
        }

        // 解密token
        String salt;
        String remoteAddr = request.getRemoteAddr();
        if (StringUtils.isBlank(remoteAddr)){
            salt = "127.0.0.1";
        }else {
            salt = remoteAddr;
        }

        Map<String, Object> gmallMap = JwtUtil.decode(token, "gmall", salt);

        if (gmallMap == null){
            returnMap.put("result","failed");
            return returnMap;
        }else {
            String nickname = (String) gmallMap.get("nickname");
            String memberId = (String) gmallMap.get("memberId");
            returnMap.put("nickname",nickname);
            returnMap.put("memberId",memberId);
            returnMap.put("result","success");
        }


        return returnMap;
    }

}
