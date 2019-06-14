package com.mod.loan.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import com.mod.loan.service.*;
import com.mod.loan.util.TimeUtils;
import com.mod.loan.util.XmlUtils;
import com.mod.loan.util.kuaiqianutil.common.KuaiqianHttpUtil;
import com.mod.loan.util.kuaiqianutil.notice.NotifyRequest;
import com.mod.loan.util.kuaiqianutil.notice.Pay2bankNotify;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankOrder;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankResult;
import com.mod.loan.util.kuaiqianutil.query.Pay2bankSearchRequestParam;
import com.mod.loan.util.kuaiqianutil.query.Pay2bankSearchResult;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Date;

@Service
public class KuaiqianServiceImpl extends BaseServiceImpl<OrderPay, String> implements KuaiqianService {
    private static final Logger logger = LoggerFactory.getLogger(KuaiqianServiceImpl.class);

    @Value("${kuaiqian.pay.url:}")
    private String kuaiqian_pay_url;
    @Value("${kuaiqian.pay.query.url:}")
    private String kuaiqian_pay_query_url;
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
    public void kuaiqianPay(OrderPayMessage payMessage) {
        String jsonStr = null;
        try {
            Order order = orderService.selectByPrimaryKey(payMessage.getOrderId());
            // 放款中的订单才能放款
            if (order.getStatus() != 22) {
                logger.info("订单放款，无效的订单状态 message={}", JSON.toJSONString(payMessage));
                return;
            }
            Merchant merchant = merchantService.findMerchantByAlias(order.getMerchant());
            UserBank userBank = userBankService.selectUserCurrentBankCard(order.getUid());
            User user = userService.selectByPrimaryKey(order.getUid());

            String serials_no = String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5), user.getId());
            BigDecimal amount = order.getActualMoney();
            // 测试放款一毛钱
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = new BigDecimal("0.1");
            }

            Pay2bankOrder payOrder = new Pay2bankOrder();
            //商家订单号 必填
            payOrder.setOrderId(serials_no);
            //金额（分） 必填
            payOrder.setAmount(String.valueOf((amount.multiply(new BigDecimal("100"))).intValue()));
            //银行名称 必填
            payOrder.setBankName(userBank.getCardName());
            //收款人姓名  必填
            payOrder.setCreditName(user.getUserName());
            //银行卡号 必填
            payOrder.setBankAcctId(userBank.getCardNo());
            String orderXml = XmlUtils.convertToXml(payOrder, "UTF-8");
            //生成pki加密报文
            logger.info("快钱放款加密开始");
            String pkiMsg = KuaiqianHttpUtil.genPayPKIMsg(orderXml, merchant.getKqMerchantCode());
            //获取请求响应的加密数据
            String sealMsg = KuaiqianHttpUtil.invokeCSSCollection(pkiMsg, kuaiqian_pay_url);
            //返回的加密报文解密
            Pay2bankResult pay2bankResult = KuaiqianHttpUtil.unsealMsgPay(sealMsg, merchant.getKqMerchantCode());

            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(serials_no);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            // 类型：快钱
            orderPay.setPayType(6);
            orderPay.setPayMoney(amount);
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());
            if ("0000".equals(pay2bankResult.getResponseBody().getErrorCode())) {
                orderPay.setUpdateTime(new Date());
                // 受理成功,插入打款流水，不改变订单状态
                orderPay.setPayStatus(1);
                orderService.updatePayInfo(null, orderPay);
                logger.info("快钱放款受理成功");
                // 受理成功，将消息存入死信队列
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait,
                        new OrderPayQueryMessage(serials_no, merchant.getMerchantAlias(), payMessage.getPayType()));
            } else {
                logger.error("快钱放款受理失败,message={}, result={}, msg={}", JSON.toJSONString(payMessage), JSONObject.toJSON(pay2bankResult), pay2bankResult.getResponseBody().getErrorMsg());
                orderPay.setRemark(pay2bankResult.getResponseBody().getErrorMsg());
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(2);
                Order record = new Order();
                record.setId(order.getId());
                record.setStatus(23);
                orderService.updatePayInfo(record, orderPay);

                //快钱商户给用户放款失败,发送短信提醒
                if ("6001".equals(pay2bankResult.getResponseBody().getErrorCode())) {
                    sendSmsMessage(merchant.getMerchantAlias(), "账户余额不足");
                }
            }
        } catch (Exception e) {
            logger.error("快钱订单放款异常， message={}，jsonStr={}", JSON.toJSONString(payMessage), jsonStr);
            logger.error("快钱订单放款异常，e={}", e);
        } finally {
            // 释放锁
            redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
        }
    }

    @Override
    public void kuaiqianPayQuery(OrderPayQueryMessage payResultMessage) {
        try {
            String payNo = payResultMessage.getPayNo();
            Merchant merchant = merchantService.findMerchantByAlias(payResultMessage.getMerchantAlias());

            Pay2bankSearchRequestParam payOrder = new Pay2bankSearchRequestParam();
            //页码 必填 正整数
            payOrder.setTargetPage("1");
            //每页条数  必填  1-20  正整数
            payOrder.setPageSize("20");
            //商家订单号
            payOrder.setOrderId(payNo);
            //开始时间 必填
            payOrder.setStartDate(TimeUtils.parseTime(TimeUtils.getYesterday(), TimeUtils.dateformat1));
            //结束时间 必填  结束-开始<=7天
            payOrder.setEndDate(TimeUtils.parseTime(TimeUtils.getTomorrow(), TimeUtils.dateformat1));
            String orderXml = XmlUtils.convertToXml(payOrder, "UTF-8");
            //生成pki加密报文
            String pkiMsg = KuaiqianHttpUtil.genPayQueryPKIMsg(orderXml, merchant.getKqMerchantCode());
            //获取请求响应的加密数据
            String sealMsg = KuaiqianHttpUtil.invokeCSSCollection(pkiMsg, kuaiqian_pay_query_url);
            //返回的加密报文解密
            Pay2bankSearchResult pay2bankResult = KuaiqianHttpUtil.unsealMsgPayQuery(sealMsg, merchant.getKqMerchantCode());
            String msg = pay2bankResult.getResultList().get(0).getErrorMsg();
            if ("0000".equals(pay2bankResult.getResultList().get(0).getErrorCode())) {
                String state = pay2bankResult.getResultList().get(0).getStatus();
                // 交易成功
                if ("111".equals(state)) {
                    logger.info("快钱放款成功");
                    paySuccess(payNo);
                    // 交易失败
                } else if ("112".equals(state)) {
                    logger.error("快钱放款失败,payResultMessage={},快钱返回结果={}", JSON.toJSONString(payResultMessage), JSONObject.toJSON(pay2bankResult));
                    payFail(payNo, msg);
                } else { // 继续查询
                    payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                    if (payResultMessage.getTimes() < 6) {
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                    } else {
                        logger.info("查询订单,payResultMessage={},快钱返回结果={},resultMsg={}",
                                JSON.toJSONString(payResultMessage), JSONObject.toJSON(pay2bankResult), msg);
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
                    }
                }
            } else {
                logger.info("查询代付结果失败,payNo={},快钱返回结果={},msg={}", payNo, msg);
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
            }
        } catch (Exception e) {
            logger.error("快钱查询代付结果异常，payResultMessage={}", JSON.toJSONString(payResultMessage));
            logger.error("快钱查询代付结果异常", e);
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query, payResultMessage);
        }
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

    /**
     * 打款失败短信验证码发送
     */
    private void sendSmsMessage(String merchant, String msg) {
        rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(merchant, "2004", "13979127403", msg));
        rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(merchant, "2004", "18072878602", msg));
        rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(merchant, "2004", "15757127746", msg));
        rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(merchant, "2004", "18958106941", msg));
    }
}
