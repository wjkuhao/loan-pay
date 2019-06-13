package com.mod.loan.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.mod.loan.common.enums.ChangjiePayOrRepayOrQueryReturnCodeEnum;
import com.mod.loan.common.enums.MerchantEnum;
import com.mod.loan.common.enums.OrderEnum;
import com.mod.loan.common.enums.SmsTemplate;
import com.mod.loan.common.mapper.BaseServiceImpl;
import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.common.message.OrderPayQueryMessage;
import com.mod.loan.common.message.QueueSmsMessage;
import com.mod.loan.config.Constant;
import com.mod.loan.config.rabbitmq.RabbitConst;
import com.mod.loan.config.redis.RedisConst;
import com.mod.loan.config.redis.RedisMapper;
import com.mod.loan.model.*;
import com.mod.loan.model.request.TransCode4PayRequest;
import com.mod.loan.model.request.TransCode4QueryRequest;
import com.mod.loan.service.*;
import com.mod.loan.util.RandomUtils;
import com.mod.loan.util.TimeUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;

@Service
public class OrderChangjiePayServiceImpl extends BaseServiceImpl<OrderPay, String> implements OrderChangjiePayService {
    private static final Logger logger = LoggerFactory.getLogger(OrderChangjiePayServiceImpl.class);
    @Autowired
    ChangjiePayService changjiePayService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderPayService orderPayService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserBankService userBankService;
    @Autowired
    private RedisMapper redisMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void changjiePay(OrderPayMessage payMessage) {
        logger.info("#[畅捷订单放款]-[开始]-payMessage={}", JSONObject.toJSON(payMessage));
        try {
            //查询待放款订单信息
            Order order = orderService.selectByPrimaryKey(payMessage.getOrderId());
            logger.info("#[查询待放款订单信息]-order={}", JSONObject.toJSON(order));
            //放款中的订单才能放款
            if (!OrderEnum.LOANING.getCode().equals(order.getStatus())) {
                logger.info("该订单的状态不是放款中");
                return;
            }
            //获取商户信息
            Merchant merchant = merchantService.findMerchantByAlias(order.getMerchant());
            if (null == merchant || StringUtils.isBlank(merchant.getCjPartnerId()) || StringUtils.isBlank(merchant.getCjPublicKey()) || StringUtils.isBlank(merchant.getCjMerchantPrivateKey())) {
                logger.info("#[该商户信息异常]-merchant={}", JSONObject.toJSON(merchant));
                return;
            }
            //获取该订单的银行卡号信息
            UserBank userBank = userBankService.selectUserCurrentBankCard(order.getUid());
            logger.info("#[获取该订单的银行卡号信息]-userBank={}", JSONObject.toJSON(userBank));
            //获取用户信息
            User user = userService.selectByPrimaryKey(order.getUid());
            String amount = order.getActualMoney().toString();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "0.01";
            }
            //唯一流水号
            String seriesNo = "p" + (TimeUtils.parseTime(new Date(), TimeUtils.dateformat5) + RandomUtils.generateRandomNum(6));
            TransCode4PayRequest transCode4PayRequest = new TransCode4PayRequest();
            transCode4PayRequest.setRequestSeriesNo(seriesNo);
            transCode4PayRequest.setBankName(userBank.getCardName());
            transCode4PayRequest.setBankCardNo(userBank.getCardNo());
            transCode4PayRequest.setName(user.getUserName());
            transCode4PayRequest.setAmount(new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP));
            transCode4PayRequest.setPartnerId(merchant.getCjPartnerId());
            transCode4PayRequest.setPrivateKey(merchant.getCjMerchantPrivateKey());
            transCode4PayRequest.setPublicKey(merchant.getCjPublicKey());
            //去调畅捷代付放款
            String result = changjiePayService.transCode4Pay(transCode4PayRequest);
            if (null == result) {
                logger.info("#[去调畅捷代付放款]-[返回结果为空]");
                return;
            }
            //落支付记录
            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(seriesNo);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            orderPay.setPayMoney(new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP));
            orderPay.setPayType(MerchantEnum.CHANGJIE.getCode());
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());
            //解析返回结果
            JSONObject jsonObject = JSONObject.parseObject(result);
            //畅捷代付放款受理成功--已生成支付单，付款处理中（交易成功，不是指付款成功，是指流程正常）
            if (StringUtils.equals("S", jsonObject.getString("AcceptStatus")) && (StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.SUCCESS_00019999.getCode(), jsonObject.getString("AppRetcode")) || StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.DOING_01019999.getCode(), jsonObject.getString("AppRetcode")))) {
                orderPay.setUpdateTime(new Date());
                //受理成功,插入打款流水，不改变订单状态
                orderPay.setPayStatus(1);
                orderService.updatePayInfo(null, orderPay);
                //受理成功，将消息存入队列，10分钟(畅捷)后去查询是否放款成功
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long,
                        new OrderPayQueryMessage(seriesNo, merchant.getMerchantAlias(), payMessage.getPayType()));
            } else {
                //放款失败
                orderPay.setRemark(jsonObject.getString("AppRetMsg"));
                orderPay.setUpdateTime(new Date());
                //受理失败
                orderPay.setPayStatus(2);
                //更新订单状态
                Order record = new Order();
                record.setId(order.getId());
                //放款失败
                record.setStatus(OrderEnum.LOAN_FAILED.getCode());
                orderService.updatePayInfo(record, orderPay);
            }
            logger.info("#[畅捷订单放款]-[结束]");
        } catch (Exception e) {
            logger.error("#[畅捷订单放款]-[异常]-e={}", e);
        } finally {
            // 释放锁
            redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
        }
    }

    @Override
    public void changjiePayQuery(OrderPayQueryMessage payResultMessage) {
        logger.info("#[畅捷订单放款结果查询]-[开始]-payResultMessage={}", JSONObject.toJSON(payResultMessage));
        //放款流水号
        String payNo = payResultMessage.getPayNo();
        //根据放款流水号查询放款流水记录
        OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
        if (null == orderPay) {
            logger.info("根据放款流水号查询放款流水记录为空-payNo={}", payNo);
            return;
        }
        if (1 < orderPay.getPayStatus()) {
            logger.info("该放款流水记录放款状态异常-payNo={},status={}", payNo, orderPay.getPayStatus());
            return;
        }
        //查询待放款订单信息
        Order order = orderService.selectByPrimaryKey(orderPay.getOrderId());
        logger.info("#[查询待放款订单信息]-order={}", JSONObject.toJSON(order));
        //放款中的订单才能放款
        if (!OrderEnum.LOANING.getCode().equals(order.getStatus())) {
            logger.info("该订单的状态不是放款中");
            return;
        }
        //获取商户信息
        Merchant merchant = merchantService.findMerchantByAlias(payResultMessage.getMerchantAlias());
        if (null == merchant || StringUtils.isBlank(merchant.getCjPartnerId()) || StringUtils.isBlank(merchant.getCjPublicKey()) || StringUtils.isBlank(merchant.getCjMerchantPrivateKey())) {
            logger.info("#[该商户信息异常]-merchant={}", JSONObject.toJSON(merchant));
            return;
        }
        //唯一流水号
        String seriesNo = "p" + (TimeUtils.parseTime(new Date(), TimeUtils.dateformat5) + RandomUtils.generateRandomNum(6));
        TransCode4QueryRequest transCode4QueryRequest = new TransCode4QueryRequest();
        transCode4QueryRequest.setRequestSeriesNo(seriesNo);
        transCode4QueryRequest.setSeriesNo(payNo);
        transCode4QueryRequest.setPartnerId(merchant.getCjPartnerId());
        transCode4QueryRequest.setPrivateKey(merchant.getCjMerchantPrivateKey());
        transCode4QueryRequest.setPublicKey(merchant.getCjPublicKey());
        //去调畅捷代付放款结果查询
        String result = changjiePayService.transCode4Query(transCode4QueryRequest);
        if (null == result) {
            logger.info("#[去调畅捷代付放款结果查询]-[返回结果为空]");
            return;
        }
        //解析返回结果
        JSONObject jsonObject = JSONObject.parseObject(result);
        //畅捷代付放款成功
        if (StringUtils.equals("S", jsonObject.getString("AcceptStatus")) && (StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.SUCCESS_0000.getCode(), jsonObject.getString("PlatformRetCode"))) && (StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.SUCCESS_000000.getCode(), jsonObject.getString("OriginalRetCode")))) {
            paySuccess(payNo);
        }
        //畅捷代付放款失败
        else if (StringUtils.equals("F", jsonObject.getString("AcceptStatus"))) {
            payFail(payNo, jsonObject.getString("AppRetMsg"));
        }
        //畅捷代付放款失败
        else if (StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.FAIL_1000.getCode(), jsonObject.getString("PlatformRetCode")) || StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.FAIL_2004.getCode(), jsonObject.getString("PlatformRetCode")) || StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.FAIL_2009.getCode(), jsonObject.getString("PlatformRetCode"))) {
            payFail(payNo, jsonObject.getString("PlatformErrorMessage"));
        }
        //畅捷代付放款失败
        else if (StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.FAIL_111111.getCode(), jsonObject.getString("OriginalRetCode")) || StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.FAIL_000005.getCode(), jsonObject.getString("OriginalRetCode")) || StringUtils.equals(ChangjiePayOrRepayOrQueryReturnCodeEnum.FAIL_000006.getCode(), jsonObject.getString("OriginalRetCode"))) {
            payFail(payNo, jsonObject.getString("OriginalErrorMessage"));
        }
        //畅捷代付放款处理中
        else {
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
        }
        logger.info("#[畅捷订单放款结果查询]-[结束]");
    }

    private void paySuccess(String payNo) {
        OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
        if (orderPay.getPayStatus() == 1) {// 只处理受理中的状态
            Order order = orderService.selectByPrimaryKey(orderPay.getOrderId());
            Order order1 = new Order();
            order1.setId(order.getId());
            order1.setArriveTime(new Date());
            Date repayTime = new DateTime(order1.getArriveTime()).plusDays(order.getBorrowDay() - 1).toDate();
            order1.setRepayTime(repayTime);
            order1.setStatus(31);

            OrderPay orderPay1 = new OrderPay();
            orderPay1.setPayNo(payNo);
            orderPay1.setPayStatus(3);
            orderPay1.setUpdateTime(new Date());
            orderService.updatePayCallbackInfo(order1, orderPay1);
            // 给用户短信通知 放款成功
            User user = userService.selectByPrimaryKey(order.getUid());
            QueueSmsMessage smsMessage = new QueueSmsMessage();
            smsMessage.setClientAlias(order.getMerchant());
            smsMessage.setType(SmsTemplate.T2001.getKey());
            smsMessage.setPhone(user.getUserPhone());
            smsMessage.setParams(order.getActualMoney() + "|" + new DateTime(repayTime).toString("MM月dd日"));
            rabbitTemplate.convertAndSend(RabbitConst.queue_sms, smsMessage);
        } else {
            logger.error("查询代付结果异常,payNo={}", payNo);
        }
    }

    private void payFail(String payNo, String errorMsg) {
        OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
        if (orderPay.getPayStatus() == 1) {// 只处理受理中的状态
            Order order1 = new Order();
            order1.setId(orderPay.getOrderId());
            order1.setStatus(23);

            OrderPay orderPay1 = new OrderPay();
            orderPay1.setPayNo(payNo);
            orderPay1.setPayStatus(4);
            orderPay1.setRemark(errorMsg);
            orderPay1.setUpdateTime(new Date());
            orderService.updatePayCallbackInfo(order1, orderPay1);
            redisMapper.unlock(RedisConst.ORDER_LOCK + orderPay.getOrderId());
        } else {
            logger.error("查询代付结果异常,payNo={}", payNo);
        }
    }
}
