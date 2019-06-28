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
import com.mod.loan.http.OkHttpReader;
import com.mod.loan.model.*;
import com.mod.loan.service.*;
import com.mod.loan.util.*;
import com.mod.loan.util.heliutil.HeliPayUtils;
import com.mod.loan.util.heliutil.vo.OrderQueryResVo;
import com.mod.loan.util.heliutil.vo.OrderResVo;
import com.mod.loan.util.huijuutil.HttpClientUtil;
import com.mod.loan.util.huijuutil.Md5_Sign;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class OrderPayServiceImpl extends BaseServiceImpl<OrderPay, String> implements OrderPayService {

    private static final Logger logger = LoggerFactory.getLogger(OrderPayServiceImpl.class);
    private static final String fuiou_reqtype = "payforreq";

    @Value("${helipay.transfer.url:}")
    private String helipay_transfer_url;
    @Value("${fuiou.requrl:}")
    private String fuiou_requrl;
    @Value("${huiju.pay.url:}")
    private String huiju_pay_url;
    @Value("${fuiou.requrl.query:}")
    private String fuiou_requrl_query;
    @Value("${huiju.pay.query.url:}")
    private String huiju_pay_query_url;
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
    private BankService bankService;
    @Autowired
    private RedisMapper redisMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private YeepayService yeepayService;
    @Autowired
    private OkHttpReader okHttpReader;
    @Autowired
    private HelipayEntrustedPayService helipayEntrustedPayService;
    @Autowired
    private SmsService smsService;

    @Override
    public void helibaoPay(OrderPayMessage payMessage) {
        try {
            Order order = orderService.selectByPrimaryKey(payMessage.getOrderId());
            if (order.getStatus() != 22) { // 放款中的订单才能放款
                logger.info("订单放款，无效的订单状态 message={}", JSON.toJSONString(payMessage));
                return;
            }

            if (!checkPayCondition(order)) {
                return;
            }

            Merchant merchant = merchantService.findMerchantByAlias(order.getMerchant());
            UserBank userBank = userBankService.selectUserCurrentBankCard(order.getUid());
            User user = userService.selectByPrimaryKey(order.getUid());
            //合利宝委托代付配置不为空
            if (StringUtils.isNotEmpty(merchant.getHlbEntrustedPrivateKey()) && StringUtils.isNotEmpty(merchant.getHlbEntrustedSignKey())) {
                helibaoEntrustedPay(order, merchant, userBank, user, payMessage);
            } else {
                helibaoPay(order, merchant, userBank, user, payMessage);
            }
        } catch (Exception e) {
            logger.error("合利宝订单放款异常, message={}", JSON.toJSONString(payMessage));
            logger.error("合利宝订单放款异常, error={}", e);
        }
    }

    /**
     * 合利宝代付放款
     */
    public void helibaoPay(Order order, Merchant merchant, UserBank userBank, User user, OrderPayMessage payMessage) {
        try {
            String serials_no = String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5),
                    user.getId());
            String amount = order.getActualMoney().toString();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "0.1";
            }
            LinkedHashMap<String, String> sPara = new LinkedHashMap<String, String>();
            sPara.put("P1_bizType", "Transfer");// 请求类型
            sPara.put("P2_orderId", serials_no);// 请求编号
            sPara.put("P3_customerNumber", merchant.getHlb_id());// 商户编号
            sPara.put("P4_amount", amount);// 订单金额

            Bank bank = bankService.selectByPrimaryKey(userBank.getCardCode());
            if (null == bank) {
                logger.info("放款失败,找不到对应uid为{}的银行卡名称{},订单号为{}", user.getId(), userBank.getCardName(), order.getId());
                return;
            }

            sPara.put("P5_bankCode", bank.getCodeHelipay());// 银行编码
            sPara.put("P6_bankAccountNo", userBank.getCardNo());// 银行账户号
            sPara.put("P7_bankAccountName", user.getUserName());// 银行账户名
            sPara.put("P8_biz", "B2C");// 业务，b2b,b2c等
            sPara.put("P9_bankUnionCode", "");// 联行号
            sPara.put("P10_feeType", "PAYER");// 手续费收取方（RECEIVER）
            sPara.put("P11_urgency", "true");// true加急
            sPara.put("P12_summary", "");// 打款备注

            JSONObject result = HeliPayUtils.requestRSA(helipay_transfer_url, sPara, merchant.getHlb_rsa_private_key());
            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(serials_no);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            orderPay.setPayMoney(new BigDecimal(amount));
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());

            if ("0000".equals(result.getString("rt2_retCode"))) {
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(1);// 受理成功,插入打款流水，不改变订单状态
                orderService.updatePayInfo(null, orderPay);
                // 受理成功，将消息存入死信队列，5秒后去查询是否放款成功
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait,
                        new OrderPayQueryMessage(serials_no, merchant.getMerchantAlias(), payMessage.getPayType()));
            } else {
                logger.error("合利宝放款受理失败,message={}, result={}", JSON.toJSONString(payMessage),
                        JSON.toJSONString(result));
                String msg = result.getString("rt3_retMsg");
                orderPay.setRemark(msg);
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(2);
                Order record = new Order();
                record.setId(order.getId());
                record.setStatus(23);
                record.setUpdateTime(new Date());
                orderService.updatePayInfo(record, orderPay);
                redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());

                //合利宝商户给用户放款失败,发送短信提醒
                if (msg.contains("商户账户余额不足")) {
                    sendSmsMessage(merchant.getMerchantAlias(), msg);
                }

            }
        } catch (Exception e) {
            logger.error("合利宝订单放款异常, message={}", JSON.toJSONString(payMessage));
            logger.error("合利宝订单放款异常", e);
        } finally {
            // 释放锁
            redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
        }
    }

    /**
     * 合利宝委托代付放款
     */
    public void helibaoEntrustedPay(Order order, Merchant merchant, UserBank userBank, User user, OrderPayMessage payMessage) {
        try {
            logger.info("helibaoEntrustedPay:{}", JSON.toJSONString(payMessage));
            String payNo = String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5),
                    user.getId());
            String amount = order.getActualMoney().toString();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "1.1";
            }
            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(payNo);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            orderPay.setPayMoney(new BigDecimal(amount));
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());
            OrderResVo resVo = null;
            logger.info("hlbEntrustedCuid:{}", userBank.getHlbEntrustedCuid());
            //用户未完成委托代付注册
            if (StringUtils.isEmpty(userBank.getHlbEntrustedCuid())) {
                resVo = helipayEntrustedPayService.bindUserCardPay(payNo, user, userBank, order, merchant);
            } else {
                resVo = helipayEntrustedPayService.entrustedPay(payNo, amount, user, userBank, merchant);
            }
            //判断是否成功
            if ("0000".equals(resVo.getRt2_retCode())) {
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(1);// 受理成功,插入打款流水，不改变订单状态
                orderPay.setRemark(resVo.getRt3_retMsg());
                orderService.updatePayInfo(null, orderPay);
                // 受理成功，将消息存入死信队列，5秒后去查询是否放款成功,这里如果是合利宝委托代付,把UID带过去吧
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait,
                        new OrderPayQueryMessage(payNo, user.getId(), merchant.getMerchantAlias(), payMessage.getPayType()));
            } else {
                logger.error("合利宝委托放款受理失败,message={}, result={}", JSON.toJSONString(payMessage),
                        JSON.toJSONString(resVo));
                String msg = resVo.getRt3_retMsg();
                orderPay.setRemark(msg);
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(2);
                Order record = new Order();
                record.setId(order.getId());
                record.setStatus(23);
                record.setUpdateTime(new Date());
                orderService.updatePayInfo(record, orderPay);
                redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());

                //合利宝商户给用户放款失败,发送短信提醒
                if (msg.contains("账户余额不足")) {
                    sendSmsMessage(merchant.getMerchantAlias(), msg);
                }
            }
        } catch (Exception e) {
            logger.error("合利宝委托代付订单放款异常, message={}", JSON.toJSONString(payMessage));
            logger.error("合利宝委托代付订单放款异常", e);
        } finally {
            // 释放锁
            redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
        }
    }


    @Override
    public void fuyouPay(OrderPayMessage payMessage) {
        String jsonStr = null;
        try {
            Order order = orderService.selectByPrimaryKey(payMessage.getOrderId());
            if (order.getStatus() != 22) { // 放款中的订单才能放款
                logger.info("订单放款，无效的订单状态 message={}", JSON.toJSONString(payMessage));
                return;
            }

            if (!checkPayCondition(order)) {
                return;
            }

            Merchant merchant = merchantService.findMerchantByAlias(order.getMerchant());
            UserBank userBank = userBankService.selectUserCurrentBankCard(order.getUid());
            User user = userService.selectByPrimaryKey(order.getUid());

            String serials_no = String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5),
                    user.getId());
            BigDecimal amount = order.getActualMoney();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = new BigDecimal("2.01"); // 测试放款一毛钱
            }

            FuYouPayBean fuYouPayBean = new FuYouPayBean();
            fuYouPayBean.setVer("1.00");
            fuYouPayBean.setMerdt(new DateTime().toString(TimeUtils.dateformat4));
            fuYouPayBean.setOrderno(serials_no);
            fuYouPayBean.setBankno("0103");
            fuYouPayBean.setCityno("1000");
            fuYouPayBean.setAccntno(userBank.getCardNo());
            fuYouPayBean.setAccntnm(user.getUserName());
            fuYouPayBean.setAmt(amount.multiply(new BigDecimal("100")).longValue());// 富友金额单位为：分
            String xml = XmlUtils.convertToXml(fuYouPayBean, "utf-8");

            String macSource = merchant.getFuyou_merid() + "|" + merchant.getFuyou_secureid() + "|" + fuiou_reqtype + "|" + xml;
            String mac = MD5.toMD5(macSource, "UTF-8").toUpperCase();
            // 切为okhttp3
            Map<String, Object> params = new HashMap<>();
            params.put("merid", merchant.getFuyou_merid());
            params.put("reqtype", fuiou_reqtype);
            params.put("xml", xml);
            params.put("mac", mac);
            String xmlResult = okHttpReader.postForm(fuiou_requrl, params, null);
            logger.info("fuyouPay付款请求提交: xmlResult={}, message={}", xmlResult, payMessage);
            //
            // 只要有付款请求 一定要有记录
            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(serials_no);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            orderPay.setPayType(2); // 类型：富友
            orderPay.setPayMoney(amount);
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());
            //
            if (!"".equals(xmlResult)) {
                Document document = DocumentHelper.parseText(xmlResult);
                String result = document.selectSingleNode("/payforrsp/ret").getStringValue();
                String msg = document.selectSingleNode("/payforrsp/memo").getStringValue();

                // 正常返回
                if ("000000".equals(result) || "AAAAAA".equals(result)) {
                    orderPay.setUpdateTime(new Date());
                    orderPay.setPayStatus(1);// 受理成功,插入打款流水，不改变订单状态
                    orderService.updatePayInfo(null, orderPay);
                    // 受理成功，将查询付款状态消息存入死信队列
                    rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait,
                            new OrderPayQueryMessage(serials_no, merchant.getMerchantAlias(), payMessage.getPayType()));
                } else {
                    // 返回结果异常
                    logger.error("富友放款受理失败,message={}, result={}, msg={}", JSON.toJSONString(payMessage), result, msg);
                    orderPay.setRemark(msg);
                    orderPay.setUpdateTime(new Date());
                    orderPay.setPayStatus(2);
                    Order record = new Order();
                    record.setId(order.getId());
                    record.setStatus(23);
                    record.setUpdateTime(new Date());
                    orderService.updatePayInfo(record, orderPay);
                }
            } else {
                // okhttp请求失败 要核查 可能付款请求已经发送成功 没有接收到响应
                logger.error("富友放款请求失败,message={}", JSON.toJSONString(payMessage));
                orderPay.setRemark("请求失败");
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(2);
                orderService.updatePayInfo(null, orderPay);
                // 请求失败 等待后续查询接过来最终确定 打款是否成功
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait,
                        new OrderPayQueryMessage(serials_no, merchant.getMerchantAlias(), payMessage.getPayType()));
            }
        } catch (Exception e) {
            logger.error("富友订单放款异常, message={}, jsonStr={}", JSON.toJSONString(payMessage), jsonStr);
            logger.error("富友订单放款异常, error={}", e.getMessage());
        } finally {
            // 释放锁
            redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
        }
    }

    @Override
    public void huijuPay(OrderPayMessage payMessage) {
        try {
            Order order = orderService.selectByPrimaryKey(payMessage.getOrderId());
            if (order.getStatus() != 22) { // 放款中的订单才能放款
                logger.info("订单放款，无效的订单状态 message={}", JSON.toJSONString(payMessage));
                return;
            }
            if (!checkPayCondition(order)) {
                return;
            }

            Merchant merchant = merchantService.findMerchantByAlias(order.getMerchant());
            UserBank userBank = userBankService.selectUserCurrentBankCard(order.getUid());
            User user = userService.selectByPrimaryKey(order.getUid());

            String serials_no = String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5),
                    user.getId());
            String amount = order.getActualMoney().toString();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "0.1";
            }

            Map<String, Object> map = new HashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            map.put("userNo", merchant.getHuiju_id());// 商户编号
            map.put("productCode", "BANK_PAY_COMPOSE_ORDER");// 产品类型
            map.put("requestTime", sdf.format(new Date())); // 交易请求时间
            map.put("merchantOrderNo", serials_no);// 商户订单号
            map.put("receiverAccountNoEnc", userBank.getCardNo()); // 收款账户号
            map.put("receiverNameEnc", user.getUserName()); // 收款人
            map.put("receiverAccountType", "201");// 账户类型
            map.put("receiverBankChannelNo", ""); // 收款账户联行号
            map.put("paidAmount", amount); // 交易金额
            map.put("currency", "201");// 币种
            map.put("isChecked", "202");// 是否复核
            map.put("paidDesc", "");// 代付说明
            map.put("paidUse", "202");// 代付用途(工资资金 201 活动经费 202 养老金 203 货款 204 劳务费 205 保险理财 206 其他 其他的代付用途，请求联系业务支持人员)
            map.put("callbackUrl", "");// 商户通知地址
            map.put("firstProductCode", "BANK_PAY_DAILY_ORDER");// 优先使用产品
            map.put("hmac", Md5_Sign.SignByMD5(getRequestSign(map), merchant.getHuiju_md5_key()));// 签名数据
            String reqBodyJson = JSON.toJSONString(map);
            String httpResponseJson = HttpClientUtil.sendHttpPost(huiju_pay_url, reqBodyJson);

            boolean isMatch = doResponseInfo(httpResponseJson, merchant.getHuiju_md5_key());
            if (!isMatch) {
                logger.error("汇聚代付同步返回信息验签失败。汇聚代付请求报文={}，同步返回信息={}", reqBodyJson, httpResponseJson);
            }

            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(serials_no);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            orderPay.setPayType(3); // 类型：汇聚支付
            orderPay.setPayMoney(new BigDecimal(amount));
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());

            JSONObject result = JSONObject.parseObject(httpResponseJson);
            if ("2001".equals(result.getString("statusCode"))) {
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(1);// 受理成功,插入打款流水，不改变订单状态
                orderService.updatePayInfo(null, orderPay);
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait,
                        new OrderPayQueryMessage(serials_no, merchant.getMerchantAlias(), payMessage.getPayType()));
            } else if ("2003".equals(result.getString("statusCode"))) {
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(1);// 受理成功,插入打款流水，不改变订单状态
                orderService.updatePayInfo(null, orderPay);
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long,
                        new OrderPayQueryMessage(serials_no, merchant.getMerchantAlias(), payMessage.getPayType()));
            } else {
                logger.error("汇聚放款受理失败,message={}, 同步返回信息={}", JSON.toJSONString(payMessage),
                        JSON.toJSONString(result));
                orderPay.setRemark(result.getJSONObject("data").getString("errorDesc"));
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(2);
                Order record = new Order();
                record.setId(order.getId());
                record.setStatus(23);
                record.setUpdateTime(new Date());
                orderService.updatePayInfo(record, orderPay);
            }
        } catch (Exception e) {
            logger.error("汇聚订单放款异常, message={}", JSON.toJSONString(payMessage));
            logger.error("汇聚订单放款异常, error={}", e);
        } finally {
            // 释放锁
            redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
        }
    }

    /**
     * 对单笔代付响应信息的处理
     *
     * @param httpResponseJson 响应信息json字符串
     * @param key              商户秘钥
     */
    @SuppressWarnings("unchecked")
    private boolean doResponseInfo(String httpResponseJson, String key) {
        Map<String, Object> httpResponseMap = (Map<String, Object>) JSONObject.parse(httpResponseJson);
        // 业务数据map集合
        Map<String, Object> dataMap = (Map<String, Object>) httpResponseMap.get("data");
        dataMap.put("statusCode", httpResponseMap.get("statusCode"));
        dataMap.put("message", httpResponseMap.get("message"));

        // 响应签名串
        String respSign = getResponseSign(dataMap);
        // 请求数据的加密签名
        String reqHmac = Md5_Sign.SignByMD5(respSign, key);
        // 请求数据的加密签名
        String respHmac = (String) dataMap.get("hmac");

        reqHmac = reqHmac.toUpperCase();
        respHmac = respHmac.toUpperCase();
        return reqHmac.equals(respHmac);
    }

    /**
     * 获取请求数据签名串信息 必须按新代付接口文档请求参数信息顺序来进行字符串的拼接，详情请参考新代付接口文档请求报文
     *
     * @param params 请求数据参数
     * @return 返回请求签名串
     */
    private String getRequestSign(Map<String, Object> params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(params.get("userNo")).append(params.get("productCode")).append(params.get("requestTime"))
                .append(params.get("merchantOrderNo")).append(params.get("receiverAccountNoEnc"))
                .append(params.get("receiverNameEnc")).append(params.get("receiverAccountType"))
                .append(params.get("receiverBankChannelNo")).append(params.get("paidAmount"))
                .append(params.get("currency")).append(params.get("isChecked")).append(params.get("paidDesc"))
                .append(params.get("paidUse")).append(params.get("callbackUrl")).append(params.get("firstProductCode"));
        return stringBuilder.toString();
    }

    private String getRequestSignForQuery(Map<String, Object> params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(params.get("userNo")).append(params.get("merchantOrderNo"));
        return stringBuilder.toString();
    }

    /**
     * 获取响应数据签名串信息 必须按新代付接口文档应答参数信息顺序来进行字符串的拼接，详情请参考新代付接口文档的应答报文
     *
     * @param params 响应数据参数
     * @return 返回响应签名串
     */
    private String getResponseSign(Map<String, Object> params) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(params.get("statusCode")).append(params.get("message")).append(params.get("errorCode"))
                .append(params.get("errorDesc")).append(params.get("userNo")).append(params.get("merchantOrderNo"));
        return stringBuilder.toString();
    }

    @Override
    public void helibaoPayQuery(OrderPayQueryMessage payResultMessage) {
        Merchant merchant = merchantService.findMerchantByAlias(payResultMessage.getMerchantAlias());
        if (StringUtils.isNotEmpty(merchant.getHlbEntrustedSignKey()) && StringUtils.isNotEmpty(merchant.getHlbEntrustedPrivateKey())) {
            //委托代付结果查询
            helipayEntrustedQuery(payResultMessage, merchant);
        } else {
            //代付结果查询
            helipayQuery(payResultMessage, merchant);
        }
    }

    /**
     * 合利宝代付结果查询
     */
    public void helipayQuery(OrderPayQueryMessage payResultMessage, Merchant merchant) {
        try {
            String payNo = payResultMessage.getPayNo();
            LinkedHashMap<String, String> sPara = new LinkedHashMap<String, String>();
            sPara.put("P1_bizType", "TransferQuery");
            sPara.put("P2_orderId", payNo);
            sPara.put("P3_customerNumber", merchant.getHlb_id());

            JSONObject result = HeliPayUtils.requestRSA(helipay_transfer_url, sPara, merchant.getHlb_rsa_private_key());
            String rt2_retCode = result.getString("rt2_retCode");// 请求状态
            String rt7_orderStatus = result.getString("rt7_orderStatus");// 处理状态
            if (!"0000".equals(rt2_retCode)) {
                logger.info("合利宝订单放款失败,payNo={}", payNo);
                payFail(payNo, result.getString("rt8_reason"));
                return;
            }
            // RECEIVE 已接收 INIT初始化 DOING处理中 SUCCESS成功 FAIL失败 REFUND退款
            switch (rt7_orderStatus) {
                case "SUCCESS":
                    paySuccess(payNo);
                    break;
                case "FAIL":
                    logger.error("订单放款失败,payResultMessage={},合利宝返回结果={}", JSON.toJSONString(payResultMessage), result);
                    payFail(payNo, result.getString("rt8_reason"));
                    break;
                case "REFUND":
                    logger.error("订单放款失败,payResultMessage={},合利宝返回结果={}", JSON.toJSONString(payResultMessage), result);
                    payFail(payNo, result.getString("rt8_reason"));
                    break;
                default:
                    // RECEIVE、INIT、DOING重新进入死信队列等待
                    payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                    if (payResultMessage.getTimes() < 6) {
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                    } else {
                        logger.info("查询订单,payResultMessage={},合利宝返回结果={}", JSON.toJSONString(payResultMessage), result);
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("合利宝查询代付结果异常,payResultMessage={}", JSON.toJSONString(payResultMessage));
            logger.error("合利宝查询代付结果异常", e);
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
        }
    }

    /**
     * 合利宝委托代付结果查询
     */
    public void helipayEntrustedQuery(OrderPayQueryMessage payResultMessage, Merchant merchant) {
        try {
            String payNo = payResultMessage.getPayNo();
            UserBank userBank = userBankService.selectUserCurrentBankCard(payResultMessage.getUid());
            OrderQueryResVo resVo = helipayEntrustedPayService.entrustedPayQuery(payResultMessage.getPayNo(), userBank, merchant);
            if (!"0000".equals(resVo.getRt2_retCode())) {
                logger.info("合利宝订单放款失败,payNo={}", payResultMessage.getPayNo());
                payFail(payResultMessage.getPayNo(), resVo.getRt3_retMsg());
                return;
            }
            // RECEIVE 已接收 INIT初始化 DOING处理中 SUCCESS成功 FAIL失败 REFUND退款
            switch (resVo.getRt8_orderStatus()) {
                case "SUCCESS":
                    paySuccess(payNo);
                    break;
                case "FAIL":
                    logger.error("订单放款失败,payResultMessage={},合利宝返回结果={}", JSON.toJSONString(payResultMessage), resVo);
                    payFail(payNo, resVo.getRt3_retMsg() + ":" + resVo.getRt9_desc());
                    break;
                case "REFUND":
                    logger.error("订单放款失败,payResultMessage={},合利宝返回结果={}", JSON.toJSONString(payResultMessage), resVo);
                    payFail(payNo, resVo.getRt3_retMsg() + ":" + resVo.getRt9_desc());
                    break;
                default:
                    // RECEIVE、INIT、DOING重新进入死信队列等待
                    payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                    if (payResultMessage.getTimes() < 6) {
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                    } else {
                        logger.info("查询订单,payResultMessage={},合利宝返回结果={}", JSON.toJSONString(payResultMessage), resVo);
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
                    }
                    break;
            }
        } catch (Exception e) {
            logger.error("合利宝查询委托代付结果异常,payResultMessage={}", JSON.toJSONString(payResultMessage));
            logger.error("合利宝查询委托代付结果异常", e);
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
        }
    }

    @Override
    public void fuyouPayQuery(OrderPayQueryMessage payResultMessage) {
        try {
            String payNo = payResultMessage.getPayNo();
            Merchant merchant = merchantService.findMerchantByAlias(payResultMessage.getMerchantAlias());

            FuYouPayQueryBean fuYouPayQueryBean = new FuYouPayQueryBean();
            fuYouPayQueryBean.setVer("1.1");
            fuYouPayQueryBean.setBusicd("AP01");
            fuYouPayQueryBean.setOrderno(payNo);
            fuYouPayQueryBean.setStartdt(new DateTime().minusDays(10).toString(TimeUtils.dateformat4));
            fuYouPayQueryBean.setEnddt(new DateTime().toString(TimeUtils.dateformat4));
            String xml = XmlUtils.convertToXml(fuYouPayQueryBean, "utf-8");
            String macSource = merchant.getFuyou_merid() + "|" + merchant.getFuyou_secureid() + "|" + xml;
            String mac = MD5.toMD5(macSource, "UTF-8").toUpperCase();
            //
            Map<String, Object> params = new HashMap<>();
            params.put("merid", merchant.getFuyou_merid());
            params.put("xml", xml);
            params.put("mac", mac);
            //
            String xmlResult = okHttpReader.postForm(fuiou_requrl_query, params, null);
            logger.info("fuyouPayQuery付款状态查询请求提交: xmlResult={}, message={}", xmlResult, payResultMessage);
            //
            if (!"".equals(xmlResult)) {
                Document document = DocumentHelper.parseText(xmlResult);
                String result = document.selectSingleNode("/qrytransrsp/ret").getStringValue();
                String msg = document.selectSingleNode("/qrytransrsp/memo").getStringValue();
                //
                if ("000000".equals(result)) {
                    String state = document.selectSingleNode("/qrytransrsp/trans/state").getStringValue();
                    String tpst = document.selectSingleNode("/qrytransrsp/trans/tpst").getStringValue();
                    String rspcd = document.selectSingleNode("/qrytransrsp/trans/rspcd").getStringValue();
                    String transStatusDesc = document.selectSingleNode("/qrytransrsp/trans/transStatusDesc")
                            .getStringValue();
                    String resultMsg = document.selectSingleNode("/qrytransrsp/trans/result").getStringValue();
                    //
                    // 交易未发送 或者 交易发送中
                    if ("0".equals(state) || "3".equals(state)) {// 一直查询
                        payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                        if (payResultMessage.getTimes() < 6) {
                            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                        } else {
                            logger.info("查询富友代付订单,payResultMessage={},富友返回结果={},msg={},resultMsg={}",
                                    JSON.toJSONString(payResultMessage), result, msg, resultMsg);
                            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
                        }
                    }
                    //
                    if ("1".equals(state) // 交易已发送且成功
                            && "0".equals(tpst) // 1.是 0.否（仅 AP01 有）
                            && "000000".equals(rspcd) //
                            && ("success".equals(transStatusDesc) || "chanAcceptSuccess".equals(transStatusDesc))) {
                        paySuccess(payNo);// 交易成功
                    } else if ("1".equals(state) && "1".equals(tpst)) {
                        payFail(payNo, transStatusDesc); // 交易失败
                    } else if ("1".equals(state) && "0".equals(tpst)
                            && ("000000".equals(rspcd) || "200001".equals(rspcd) || "200002".equals(rspcd)
                            || "999999".equals(rspcd) || "AAAAAA".equals(rspcd) || "null".equals(rspcd)
                            || StringUtils.isBlank(rspcd))) { // 继续查询
                        payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                        if (payResultMessage.getTimes() < 6) {
                            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                        } else {
                            logger.info("查询订单,payResultMessage={},富友返回结果={},msg={},resultMsg={}",
                                    JSON.toJSONString(payResultMessage), result, msg, resultMsg);
                            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
                        }
                    } else if ("1".equals(state) && "0".equals(tpst)) {
                        payFail(payNo, transStatusDesc); // 交易失败
                    }
                } else if ("200029".equals(result)) {// 查询返回码，未找到交易(查无此单)
                    // 为了确保再查一次是不是查不到
                    payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                    if (payResultMessage.getTimes() < 2) {// 多查一次富有
                        logger.info("查询富友代付订单,查无此单,payResultMessage={},富友返回结果={},msg={},resultMsg={}",
                                JSON.toJSONString(payResultMessage), result, msg, "查无此单");
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                    } else {// 还是找不到 说明没有提交成功
                        logger.error("查询富友代付订单,查无此单,payResultMessage={},富友返回结果={},msg={},resultMsg={}",
                                JSON.toJSONString(payResultMessage), result, msg, "查无此单");
                        paySendFail(payNo, "查无此单");
                    }
                } else {
                    logger.info("富友查询代付结果失败,payNo={},富友返回结果={},msg={}", payNo, result, msg);
                    rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                }
            } else {
                logger.error("富友查询代付请求失败,payNo={}", payNo);
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
            }
        } catch (Exception e) {
            logger.error("富友查询代付结果异常，payResultMessage={}", JSON.toJSONString(payResultMessage));
            logger.error("富友查询代付结果异常, error={}", e);
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
        }
    }

    @Override
    public void huijuPayQuery(OrderPayQueryMessage payResultMessage) {
        try {
            String payNo = payResultMessage.getPayNo();
            Merchant merchant = merchantService.findMerchantByAlias(payResultMessage.getMerchantAlias());

            Map<String, Object> map = new HashMap<>();
            map.put("userNo", merchant.getHuiju_id());// 商户编号
            map.put("merchantOrderNo", payNo);// 商户订单号
            map.put("hmac", Md5_Sign.SignByMD5(getRequestSignForQuery(map), merchant.getHuiju_md5_key()));// 签名数据
            String reqBodyJson = JSON.toJSONString(map);
            String httpResponseJson = HttpClientUtil.sendHttpPost(huiju_pay_query_url, reqBodyJson);

            boolean isMatch = doResponseInfo(httpResponseJson, merchant.getHuiju_md5_key());
            if (!isMatch) {
                logger.error("汇聚代付查询同步返回信息验签失败。汇聚代付查询请求报文={}，同步返回信息={}", reqBodyJson, httpResponseJson);
            }

            JSONObject result = JSONObject.parseObject(httpResponseJson);

            if ("2001".equals(result.getString("statusCode"))) {
                JSONObject data = result.getJSONObject("data");
                switch (data.getString("status")) {
                    case "204":
                        logger.error("订单放款失败,payResultMessage={},汇聚返回结果={}", JSON.toJSONString(payResultMessage), result);
                        payFail(payNo, result.getJSONObject("data").getString("errorDesc"));
                        break;
                    case "205":
                        paySuccess(payNo);
                        break;
                    case "208":
                        logger.error("订单放款失败,payResultMessage={},汇聚返回结果={}", JSON.toJSONString(payResultMessage), result);
                        payFail(payNo, result.getJSONObject("data").getString("errorDesc"));
                        break;
                    case "214":
                        logger.error("订单放款失败,payResultMessage={},汇聚返回结果={}", JSON.toJSONString(payResultMessage), result);
                        payFail(payNo, result.getJSONObject("data").getString("errorDesc"));
                        break;
                    default:
                        payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                        if (payResultMessage.getTimes() < 6) {
                            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                        } else {
                            logger.info("查询订单,payResultMessage={},汇聚返回结果={}", JSON.toJSONString(payResultMessage), result);
                            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
                        }
                        break;
                }
            } else {
                logger.info("代付查询失败,payResultMessage={},汇聚返回结果={}", JSON.toJSONString(payResultMessage), result);
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
            }
        } catch (Exception e) {
            logger.error("汇聚查询代付结果异常,payResultMessage={}", JSON.toJSONString(payResultMessage));
            logger.error("汇聚查询代付结果异常, error={}", e);
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
        }
    }

    @Override
    public void yeePay(OrderPayMessage payMessage) {
        try {
            Order order = orderService.selectByPrimaryKey(payMessage.getOrderId());
            if (order.getStatus() != 22) { // 放款中的订单才能放款
                logger.info("订单放款，无效的订单状态 message={}", JSON.toJSONString(payMessage));
                return;
            }

            if (!checkPayCondition(order)) {
                return;
            }

            Merchant merchant = merchantService.findMerchantByAlias(order.getMerchant());
            UserBank userBank = userBankService.selectUserCurrentBankCard(order.getUid());
            User user = userService.selectByPrimaryKey(order.getUid());

            String amount = order.getActualMoney().toString();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "0.21";
            }

            Bank bank = bankService.selectByPrimaryKey(userBank.getCardCode());
            if (null == bank) {
                logger.info("放款失败,找不到对应uid为{}的银行卡名称{},订单号为{}", user.getId(), userBank.getCardName(), order.getId());
                return;
            }

            String batchNo = TimeUtils.parseTime(new Date(), TimeUtils.dateformat5) + RandomUtils.generateRandomNum(6);
            String serials_no = "p" + batchNo;

            String errMsg = yeepayService.payToCustom(DesUtil.decryption(merchant.getYeepay_group_no()),
                    DesUtil.decryption(merchant.getYeepay_loan_appkey()), DesUtil.decryption(merchant.getYeepay_loan_private_key()),
                    batchNo, serials_no, amount, user.getUserName(), userBank.getCardNo(), bank.getCodeYeepay());

            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(serials_no);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            orderPay.setPayMoney(new BigDecimal(amount));
            orderPay.setPayType(4); // 类型：易宝
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());

            if (StringUtils.isEmpty(errMsg)) {
                logger.info("易包放款受理成功,message={}", JSON.toJSONString(payMessage));
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(1);// 受理成功,插入打款流水，不改变订单状态
                orderService.updatePayInfo(null, orderPay);
                // 受理成功，将消息存入死信队列，5秒后去查询是否放款成功
                rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait,
                        new OrderPayQueryMessage(serials_no, merchant.getMerchantAlias(), payMessage.getPayType()));
            } else {
                if ("商户可用打款余额不足".equals(errMsg)) {
                    rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(order.getMerchant(), "2004", "13979127403", String.valueOf(orderPay.getPayMoney())));
                    rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(order.getMerchant(), "2004", "13575506440", String.valueOf(orderPay.getPayMoney())));
                    rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(order.getMerchant(), "2004", "15757127746", String.valueOf(orderPay.getPayMoney())));
                    rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(order.getMerchant(), "2004", "18958106941", String.valueOf(orderPay.getPayMoney())));
                    rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(order.getMerchant(), "2004", "15274029140", String.valueOf(orderPay.getPayMoney())));
                }

                logger.error("易包放款受理失败,message={}, result={}", JSON.toJSONString(payMessage),
                        JSON.toJSONString(errMsg));
                orderPay.setRemark(errMsg);
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(2);
                Order record = new Order();
                record.setId(order.getId());
                record.setStatus(23);
                record.setUpdateTime(new Date());
                orderService.updatePayInfo(record, orderPay);
                redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
            }
        } catch (Exception e) {
            logger.error("易宝订单放款异常, message={}", JSON.toJSONString(payMessage));
            logger.error("易宝订单放款异常, error={}", e);
        }
    }

    @Override
    public void yeePayQuery(OrderPayQueryMessage payResultMessage) {
        try {
            String payNo = payResultMessage.getPayNo();
            Merchant merchant = merchantService.findMerchantByAlias(payResultMessage.getMerchantAlias());

            String batchNo = payNo.substring(1);//batchNo 去掉前面的字母
            String errMsg = yeepayService.payToCustomQuery(DesUtil.decryption(merchant.getYeepay_group_no()), DesUtil.decryption(merchant.getYeepay_loan_appkey()),
                    DesUtil.decryption(merchant.getYeepay_loan_private_key()), batchNo);

            logger.info("查询订单,payResultMessage={},易宝返回结果={}", JSON.toJSONString(payResultMessage), errMsg);
            if (errMsg != null) {
                if (errMsg.equals("processing")) {
                    payResultMessage.setTimes(payResultMessage.getTimes() + 1);
                    if (payResultMessage.getTimes() < 3) {
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
                    } else {
                        logger.info("查询订单,payResultMessage={},易宝返回结果={}", JSON.toJSONString(payResultMessage), errMsg);
                        rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payResultMessage);
                    }
                } else {
                    payFail(payNo, errMsg);
                }
            } else {
                paySuccess(payNo);
            }
        } catch (Exception e) {
            logger.error("易宝查询代付结果异常,payResultMessage={}", JSON.toJSONString(payResultMessage));
            logger.error("易宝查询代付结果异常, error={}", e);
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait, payResultMessage);
        }
    }

    @Override
    public boolean checkPayCondition(Order order) {
        if (order.getBorrowMoney().compareTo(new BigDecimal(Constant.MERCHANT_MAX_PRODUCT_MONEY)) > 0) {
            order.setStatus(23);
            order.setUpdateTime(new Date());
            logger.error("代付检查异常:orderid={},放款金额={}", order.getId(), order.getBorrowMoney());
            sendSmsMessage(order.getMerchant(), "代付检查异常:放款金额大于" + Constant.MERCHANT_MAX_PRODUCT_MONEY);
            orderService.updateByPrimaryKeySelective(order);
            return false;
        }

        int count = orderService.countOrderPaySuccessOneDay(order.getUid());
        if (count > 1) {
            order.setStatus(23);
            order.setUpdateTime(new Date());
            logger.error("代付检查异常:orderid={},一天重复放款={}", order.getId(), order.getBorrowMoney());
            sendSmsMessage(order.getMerchant(), "代付检查异常:一天重复放款");
            orderService.updateByPrimaryKeySelective(order);
            return false;
        }
        return true;
    }

    private void paySuccess(String payNo) {
        OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
        if (orderPay.getPayStatus() == 1) {// 只处理受理中的状态
            Order order = orderService.selectByPrimaryKey(orderPay.getOrderId());
            Order order1 = new Order();
            order1.setId(order.getId());
            order1.setArriveTime(new Date());
            order1.setUpdateTime(new Date());
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
            smsService.send(order.getMerchant(), SmsTemplate.T2001.getKey(), user.getUserPhone(),
                    order.getActualMoney() + "|" + new DateTime(repayTime).toString("MM月dd日"), user.getUserOrigin());
        } else {
            logger.error("查询代付结果异常,payNo={}", payNo);
        }
    }

    private void payFail(String payNo, String errorMsg) {
        OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
        if (orderPay.getPayStatus() == 1) {// 只处理受理中的状态
            Order order1 = new Order();
            order1.setId(orderPay.getOrderId());
            order1.setUpdateTime(new Date());
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
     * 代付请求失败
     *
     * @param payNo    打款单号
     * @param errorMsg 错误信息
     */
    private void paySendFail(String payNo, String errorMsg) {
        OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
        if (orderPay.getPayStatus() == 2) {// 只处理受理失败的状态
            Order order1 = new Order();
            order1.setId(orderPay.getOrderId());
            order1.setUpdateTime(new Date());
            order1.setStatus(23);

            OrderPay orderPay1 = new OrderPay();
            orderPay1.setPayNo(payNo);
            orderPay1.setPayStatus(2);
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
        rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(merchant, "2004", "15274029140", msg));
        rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(merchant, "2004", "15757127746", msg));
        rabbitTemplate.convertAndSend(RabbitConst.queue_sms, new QueueSmsMessage(merchant, "2004", "18958106941", msg));
    }
}
