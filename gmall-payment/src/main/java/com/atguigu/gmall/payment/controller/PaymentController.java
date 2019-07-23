package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.annotations.LoginRequired;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.conf.AlipayConfig;
import com.atguigu.gmall.service.OmsOrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Autowired
    AlipayClient alipayClient;

    @Reference
    OmsOrderService omsOrderService;

    @Autowired
    PaymentService paymentService;

    @RequestMapping("alipay/callback/return")
    public String callback(HttpServletRequest request){

        String out_trade_no = request.getParameter("out_trade_no");
        // 检查交易幂等性,数据库中的订单支付状态
        String dbpayStatus = paymentService.getDbpayStatus(out_trade_no);

        if (!dbpayStatus.equals("success")){
            // 获取交易信息
            String pay_amount = request.getParameter("total_amount");
            String trade_no = request.getParameter("trade_no");

            String queryString = request.getQueryString();

            // 更新支付信息
            PaymentInfo paymentInfo = new PaymentInfo();
            paymentInfo.setPaymentStatus("已支付");
            paymentInfo.setOrderSn(out_trade_no);
            paymentInfo.setAlipayTradeNo(trade_no);
            paymentInfo.setCallbackContent(queryString);
            paymentInfo.setCallbackTime(new Date());
            paymentInfo.setTotalAmount(new BigDecimal(pay_amount));

            // 发送执行更新订单信息的消息队列 PAYMENT_SUCCESS_QUEUE
            paymentService.sendPaymentResult(paymentInfo);

            // 执行更新支付信息
            paymentService.updatePaymentInfoByOrderSn(paymentInfo);
        }

        return "redirect:/finish.html";
    }

    @RequestMapping("/alipay/submit")
    @ResponseBody
    public String alipay(BigDecimal totalAmount,String orderSn){

        OmsOrder omsOrder = omsOrderService.getOrderByOrderSn(orderSn);

        // 调用支付宝接口
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        // 设置同步回调地址
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 设置异步回调地址
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);
        // 封装业务参数
        Map<String,String> requestMap = new HashMap<>();
        requestMap.put("out_trade_no", orderSn);
        requestMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        requestMap.put("total_amount", "0.01");
        requestMap.put("subject", omsOrder.getOmsOrderItems().get(0).getProductName());
        String jsonString = JSON.toJSONString(requestMap);
        alipayRequest.setBizContent(jsonString);

        String form = "";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody();
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 生成支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setOrderId(omsOrder.getId());
        paymentInfo.setOrderSn(orderSn);
        paymentInfo.setPaymentStatus("未支付");
        paymentInfo.setSubject(omsOrder.getOmsOrderItems().get(0).getProductName());
        paymentInfo.setTotalAmount(totalAmount);

        paymentService.addPaymentInfo(paymentInfo);

        // 发送检查支付状态的消息队列
        paymentService.sendCheckPayStatusQueue(orderSn,5);

        return form;
    }

    @LoginRequired(isNeedSuccess = true)
    @RequestMapping("paymentIndex")
    public String paymentIndex(String orderSn, BigDecimal totalAmount, ModelMap modelMap, HttpServletRequest request){

        String nickname = (String) request.getAttribute("nickname");
        modelMap.put("nickName",nickname);
        modelMap.put("orderSn",orderSn);
        modelMap.put("totalAmount",totalAmount);

        return "index";
    }

}
