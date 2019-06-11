package com.mod.loan.controller;

import com.alibaba.fastjson.JSONObject;
import com.mod.loan.common.enums.ResponseEnum;
import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.common.message.OrderPayQueryMessage;
import com.mod.loan.common.model.ResultMessage;
import com.mod.loan.service.KuaiqianService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin("*")
@RestController
@RequestMapping("order")
public class KuaiqianPayController {
    private static Logger logger = LoggerFactory.getLogger(KuaiqianPayController.class);
    @Autowired
    KuaiqianService kuaiqianService;

    @RequestMapping(value = "order_pay_kuaiqian")
    public ResultMessage orderPayKuaiqian(){
        ResultMessage resultMessage = null;
        logger.info("#[快钱支付打款]-[开始]");
        try {
            OrderPayMessage orderPayMessage = new OrderPayMessage();
            orderPayMessage.setOrderId(1L);
            orderPayMessage.setPayType("6");
            kuaiqianService.kuaiqianPay(orderPayMessage);
            logger.info("#[快钱支付打款]-[结束]-result={}", JSONObject.toJSON(resultMessage));
            return resultMessage;
        } catch (Exception e) {
            logger.info("#[快钱支付打款]-[异常]-e={}", e);
            resultMessage.setStatus(ResponseEnum.M4000.getCode());
            resultMessage.setMessage(ResponseEnum.M4000.getMessage());
            return resultMessage;
        }
    }

    /**
     * 查询快钱还款支付订单状态
     */
    @RequestMapping(value = "query_kuaiqian_pay_order")
    public ResultMessage queryKuaiqianPayOrder(){
        ResultMessage resultMessage = null;
        logger.info("#[查询快钱支付订单状态]-[开始]");
        try {
            OrderPayQueryMessage orderPayQueryMessage = new OrderPayQueryMessage();
            orderPayQueryMessage.setPayNo("p201906111101041");
            orderPayQueryMessage.setMerchantAlias("farm");
            orderPayQueryMessage.setPayType("6");
            kuaiqianService.kuaiqianPayQuery(orderPayQueryMessage);
            logger.info("#[查询快钱支付订单状态]-[结束]-result={}", JSONObject.toJSON(resultMessage));
            return resultMessage;
        } catch (Exception e) {
            logger.info("#[查询快钱支付订单状态]-[异常]-e={}", e);
            resultMessage.setStatus(ResponseEnum.M4000.getCode());
            resultMessage.setMessage(ResponseEnum.M4000.getMessage());
            return resultMessage;
        }
    }
}
