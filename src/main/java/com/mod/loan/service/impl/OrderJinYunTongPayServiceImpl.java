package com.mod.loan.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mod.loan.common.enums.ChangjiePayOrRepayOrQueryReturnCodeEnum;
import com.mod.loan.common.enums.MerchantEnum;
import com.mod.loan.common.enums.OrderEnum;
import com.mod.loan.common.enums.SmsTemplate;
import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.common.message.OrderPayQueryMessage;
import com.mod.loan.common.message.QueueSmsMessage;
import com.mod.loan.config.Constant;
import com.mod.loan.config.rabbitmq.RabbitConst;
import com.mod.loan.config.redis.RedisConst;
import com.mod.loan.config.redis.RedisMapper;
import com.mod.loan.mapper.OrderPayMapper;
import com.mod.loan.model.*;
import com.mod.loan.model.request.TransCode4PayRequest;
import com.mod.loan.service.*;
import com.mod.loan.util.RandomUtils;
import com.mod.loan.util.TimeUtils;
import com.mod.loan.util.jinyuntong.*;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

/**
 * 金运通打款
 *
 * @Author actor
 * @Date 2019/6/20 16:41
 */
@Service
public class OrderJinYunTongPayServiceImpl implements OrderJinYunTongPayService {
    private static final Logger log = LoggerFactory.getLogger(OrderJinYunTongPayServiceImpl.class);
    //地址
    @Value("${jytpay.appUrl}")
    private String APP_SERVER_URL;

    @Value("${jytpay.respMsgParamSeparator}")
    private String RESP_MSG_PARAM_SEPARATOR;
    /**
     * 返回报文merchant_id字段前缀
     */
    @Value("${jytpay.respMsgParamPrefixMerchantId}")
    private String RESP_MSG_PARAM_PREFIX_MERCHANT_ID;
    @Value("${jytpay.respMsgParamPrefixMsgEnc}")
    private String RESP_MSG_PARAM_PREFIX_MSG_ENC;
    /**
     * 返回报文xml_enc字段前缀
     */
    @Value("${jytpay.respMsgParamPrefixXmlEnc}")
    private String RESP_MSG_PARAM_PREFIX_XML_ENC;
    /**
     * 返回报文key_enc字段前缀
     */
    @Value("${jytpay.respMsgParamPrefixKeyEnc}")
    private String RESP_MSG_PARAM_PREFIX_KEY_ENC;
    /**
     * 返回报文sign字段前缀
     */
    @Value("${jytpay.respMsgParamPrefixSign}")
    private String RESP_MSG_PARAM_PREFIX_SIGN;
    @Value("${jytpay.payTraceCode}")
    private String payTraceCode;
    @Value("${jytpay.payQueryTraceCode}")
    private String payQueryTraceCode;
    @Autowired
    private OrderService orderService;
    @Autowired
    private MerchantService merchantService;
    @Autowired
    private UserBankService userBankService;
    @Autowired
    private UserService userService;
    @Autowired
    private RedisMapper redisMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private OrderPayMapper orderPayMapper;
    @Autowired
    private OrderPayService orderPayService;

    /**
     * 打款
     *
     * @Author actor
     * @Date 2019/6/20 16:44
     */
    @Override
    public void pay(OrderPayMessage payMessage) {
        log.info("#[金运通订单放款]-[开始]-payMessage={}", JSONObject.toJSON(payMessage));
        try {
            //查询待放款订单信息
            Order order = orderService.selectByPrimaryKey(payMessage.getOrderId());
            if (order == null) {
                log.error("金运通打款订单不存在,orderId={}", payMessage.getOrderId());
                return;
            }
            log.info("#[金运通查询待放款订单信息]-order={}", JSONObject.toJSON(order));
            //放款中的订单才能放款
            if (!OrderEnum.LOANING.getCode().equals(order.getStatus())) {
                log.info("该订单的状态不是放款中,orderId={}", order.getId());
                return;
            }
            //获取商户信息
            Merchant merchant = merchantService.findMerchantByAlias(order.getMerchant());
            if (null == merchant || StringUtils.isBlank(merchant.getJinyuntongMerchantId()) || StringUtils.isBlank(merchant.getJinyuntongMerchantPrivateKey()) || StringUtils.isBlank(merchant.getJinyuntongPublicKey())) {
                log.error("#[该订单的商户信息异常]-orderId={}", payMessage.getOrderId());
                return;
            }
            //获取该订单的银行卡号信息
            UserBank userBank = userBankService.selectUserCurrentBankCard(order.getUid());
            if (userBank == null) {
                log.error("该订单的用户银行卡为空,orderId={}", order.getId());
            }
            log.info("#[获取该订单的银行卡号信息]-userBank={}", JSONObject.toJSON(userBank));
            //获取用户信息
            User user = userService.selectByPrimaryKey(order.getUid());
            String amount = order.getActualMoney().toString();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "0.01";
            }

            RSAHelper rsaHelper = getRSAHelper(merchant.getJinyuntongMerchantId(), merchant.getJinyuntongMerchantPrivateKey(), merchant.getJinyuntongPublicKey());

            //唯一流水号
            String seriesNo = "p" + (TimeUtils.parseTime(new Date(), TimeUtils.dateformat5) + RandomUtils.generateRandomNum(6));
            TC1002_ReqBodyBean reqBean = new TC1002_ReqBodyBean();
            reqBean.setAccountName(user.getUserName());
            reqBean.setAccountNo(userBank.getCardNo());
            reqBean.setAccountType(AccoutTypeEnums.PERSON.getCode());
            reqBean.setBankName(userBank.getCardName());
            reqBean.setBsnCode(PayBsnTypeEnums.BSN_PAY_09400.getCode());
            reqBean.setCertNo(userBank.getCardNo());
            reqBean.setCertType(CertTypeEnums.IDCARD.getCode());
            reqBean.setCurrency(CurrencyTypeEnums.CNY.getCode());
            reqBean.setMerViralAcct("");
            reqBean.setMobile(userBank.getCardPhone());
            reqBean.setRemark("");
            reqBean.setBrachBankCity("");
            reqBean.setBrachBankName("");
            reqBean.setBrachBankProvince("");
            reqBean.setAgrtNo("");
            reqBean.setTranAmt(new BigDecimal(amount));
            reqBean.setReserve("");
            reqBean.setBrachBankCode(userBank.getCardCode());
            MsgHeadInfoV2 head = getHeadInfo(payTraceCode, merchant.getJinyuntongMerchantId(), "1.0.0", seriesNo);
            JSONObject jo = new JSONObject();
            jo.put(MsgHeadInfoV2.MSG_JSON_HEAD, head);
            jo.put(MsgHeadInfoV2.MSG_JSON_BODY, reqBean);

            //落支付记录
            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(seriesNo);
            orderPay.setUid(order.getUid());
            orderPay.setOrderId(order.getId());
            orderPay.setPayMoney(new BigDecimal(amount).setScale(2, BigDecimal.ROUND_HALF_UP));
            orderPay.setPayType(MerchantEnum.jinyuntong.getCode());
            orderPay.setBank(userBank.getCardName());
            orderPay.setBankNo(userBank.getCardNo());
            orderPay.setCreateTime(new Date());
            orderPay.setUpdateTime(new Date());
            orderPay.setPayStatus(0);
            orderPayMapper.insertSelective(orderPay);
            try {
                String mac = signMsg(jo.toJSONString(), rsaHelper);

                String respStr = sendMsg(jo.toJSONString(), mac, merchant.getJinyuntongMerchantId(), rsaHelper);
                log.info("金运通打款返回信息:" + respStr);
                JSONObject json = JSONObject.parseObject(respStr);
                System.out.println(json.getJSONObject("head"));//报文头内容
                System.out.println(json.getJSONObject("body"));//报文体
                String respCode = json.getJSONObject("head").get("respCode").toString();
                System.out.println(respCode);
                String tranState = json.getJSONObject("body").get("tranState").toString();
                System.out.println(tranState);
                if ("S0000000".equals(respCode) && "01".equals(tranState)) {
                    //TODO 交易成功
                    log.info("金运通打款成功,payNo={},orderId={}", seriesNo, order.getId());
                    paySyncSuccess(seriesNo);
                } else if (!respCode.equals("E0000000") && respCode.startsWith("E") && "03".equals(tranState)) {
                    //TODO 交易失败
                    log.info("金运通打款失败,payNo={},orderId={}", seriesNo, order.getId());
                    paySyncFail(seriesNo, json.getJSONObject("head").getString("respDesc"));
                } else {
                    //TODO 交易处理中
                    log.info("金运通打款受理成功,payNo={},orderId={}", seriesNo, order.getId());
                    OrderPay orderPay1 = new OrderPay();
                    orderPay1.setPayNo(seriesNo);
                    orderPay1.setUpdateTime(new Date());
                    //受理成功,插入打款流水，不改变订单状态
                    orderPay1.setPayStatus(1);
                    orderPayMapper.updateByPrimaryKeySelective(orderPay1);
                    //受理成功，将消息存入队列，10分钟(金运通)后去查询是否放款成功
                    rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long,
                            new OrderPayQueryMessage(seriesNo, merchant.getMerchantAlias(), payMessage.getPayType()));
                }

            } catch (Exception e) {
                log.error("调用金运通打款失败,e={}", e);
                paySyncFail(seriesNo, e.getMessage());
                return;
            }
            log.info("#[金运通订单放款]-[结束]");
        } catch (Exception e) {
            log.error("#[金运通订单放款]-[异常]-e={}", e);
        } finally {
            // 释放锁
            redisMapper.unlock(RedisConst.ORDER_LOCK + payMessage.getOrderId());
        }
    }

    /**
     * 打款查询
     *
     * @Author actor
     * @Date 2019/6/20 16:44
     */
    @Override
    public void payQuery(OrderPayQueryMessage payQueryMessage) {
        try {
            log.info("#[金运通订单放款结果查询]-[开始]-payResultMessage={}", JSONObject.toJSON(payQueryMessage));
            //放款流水号
            String payNo = payQueryMessage.getPayNo();
            //根据放款流水号查询放款流水记录
            OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
            if (null == orderPay) {
                log.info("根据放款流水号查询放款流水记录为空-payNo={}", payNo);
                return;
            }
            //查询待放款订单信息
            Order order = orderService.selectByPrimaryKey(orderPay.getOrderId());
            log.info("#[查询待放款订单信息]-order={}", JSONObject.toJSON(order));
            //放款中的订单才能放款
            if (!OrderEnum.LOANING.getCode().equals(order.getStatus())) {
                log.info("该订单的状态不是放款中");
                return;
            }
            //获取商户信息
            Merchant merchant = merchantService.findMerchantByAlias(payQueryMessage.getMerchantAlias());
            if (null == merchant || StringUtils.isBlank(merchant.getJinyuntongMerchantId()) || StringUtils.isBlank(merchant.getJinyuntongPublicKey()) || StringUtils.isBlank(merchant.getJinyuntongMerchantPrivateKey())) {
                log.info("#[该商户信息异常]-merchant={}", JSONObject.toJSON(merchant));
                return;
            }

            TC2002_ReqBodyBean reqBean = new TC2002_ReqBodyBean();
            reqBean.setOriTranFlowid(payNo);
            RSAHelper rsaHelper = getRSAHelper(merchant.getJinyuntongMerchantId(), merchant.getJinyuntongMerchantPrivateKey(), merchant.getJinyuntongPublicKey());
//            String merOrderId = getTranFlow();
            MsgHeadInfoV2 head = getHeadInfo(payQueryTraceCode, merchant.getJinyuntongMerchantId(), "1.0.0", payNo);
            JSONObject jo = new JSONObject();
            jo.put(MsgHeadInfoV2.MSG_JSON_HEAD, head);
            jo.put(MsgHeadInfoV2.MSG_JSON_BODY, reqBean);
            String mac = signMsg(jo.toJSONString(), rsaHelper);

            String respStr = sendMsg(jo.toJSONString(), mac, merchant.getJinyuntongMerchantId(), rsaHelper);
            JSONObject json = JSONObject.parseObject(respStr);
            System.out.println(json.getJSONObject("head"));//报文头内容
            System.out.println(json.getJSONObject("body"));//报文体
            String respCode = json.getJSONObject("head").get("respCode").toString();
            System.out.println(respCode);
            String tranRespCode = json.getJSONObject("body").get("tranRespCode").toString();
            System.out.println(tranRespCode);
            String tranState = json.getJSONObject("body").get("tranState").toString();
            String remark = json.getJSONObject("body").getString("tranRespDesc");
            System.out.println(tranState);
            if ("S0000000".equals(respCode)) {
                //查询成功
                if ("S0000000".equals(tranRespCode) && "01".equals(tranState)) {
                    log.info("交易成功,orderId={}", order.getId());
                    //TODO 交易成功
                    paySuccess(payNo);
                } else if (!tranRespCode.equals("E0000000") && tranRespCode.startsWith("E") && "03".equals(tranState)) {
                    //TODO 交易失败
                    log.info("交易失败,orderId={}", order.getId());
                    payFail(payNo, remark);
                } else {
                    //TODO 交易处理中
                    log.info("交易处理中,orderId={}", order.getId());
                    rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query_wait_long, payQueryMessage);
                }
            }
        } catch (Exception e) {
            log.error("金运通打款查询异常,e={}", e);
            rabbitTemplate.convertAndSend(RabbitConst.queue_order_pay_query, payQueryMessage);
        }

    }

    /*
     * @Author actor
     * @Date 2019/6/19 15:57
     */
    public RSAHelper getRSAHelper(String merchantId, String clientPrivateKey, String serverPublicKey) {
        RSAHelper rsaHelper = new RSAHelper();

        try {
            rsaHelper.initKey(clientPrivateKey, serverPublicKey, 2048);
            return rsaHelper;
        } catch (Exception e) {
            log.error("金运通生成rsakey异常,merchantId={},e={}", merchantId, e);
            throw new AppException("金运通生成rsakey异常");
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
            log.error("查询代付结果异常,payNo={}", payNo);
        }
    }

    private void paySyncSuccess(String payNo) {
        try {
            OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
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
        } catch (Exception e) {
            log.error("金运通打款同步返回成功,改表异常,payNo={},e={}", payNo, e);
        }
    }

    private void paySyncFail(String payNo, String errorMsg) {
        try {
            OrderPay orderPay = orderPayService.selectByPrimaryKey(payNo);
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
        } catch (Exception e) {
            log.error("金运通打款同步返回失败,改表异常,payNo={},e={}", payNo, e);
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
            log.error("查询代付结果异常,payNo={}", payNo);
        }
    }

    /**
     * 获得请求头
     *
     * @Author actor
     * @Date 2019/6/21 16:23
     */
    public MsgHeadInfoV2 getHeadInfo(String tranCode, String merchantId, String version, String merOrderId) {
        MsgHeadInfoV2 head = new MsgHeadInfoV2();
        head.setMerchantId(merchantId);
        head.setTranCode(tranCode);
        head.setTranDate(DateTimeUtils.getNowDateStr(DateTimeUtils.DATE_FORMAT_YYYYMMDD));
        head.setTranTime(DateTimeUtils.getNowDateStr(DateTimeUtils.DATETIME_FORMAT_HHMMSS));
        head.setTranFlowid(merOrderId);
        head.setTranType("01");
        head.setVersion(version);
        return head;
    }

    public String signMsg(String xml, RSAHelper rsaHelper) {
        String hexSign = null;

        try {
            byte[] sign = rsaHelper.signRSA(xml.getBytes("UTF-8"), false, "UTF-8");

            hexSign = StringUtil.bytesToHexString(sign);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            log.error("金运通生成rsa异常,e={}", e);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("金运通生成rsa异常,e={}", e);
        }

        return hexSign;
    }

    public String sendMsg(String xml, String sign, String merchantId, RSAHelper rsaHelper) throws Exception {
        log.info("金运通上送报文：" + xml);
        log.info("金运通上送签名：" + sign);

        byte[] des_key = DESHelper.generateDesKey();

        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("merchant_id", merchantId);
        paramMap.put("xml_enc", encryptXml(xml, des_key));
        paramMap.put("key_enc", encryptKey(des_key, rsaHelper));
        paramMap.put("sign", sign);

        // 获取执行结果
        String res = null;
        try {
            res = HttpClient431Util.doPost(paramMap, APP_SERVER_URL);

            if (res == null) {
                log.error("金运通服务器连接失败");

                throw new AppException("测试异常");
            } else {
                log.info("金运通连接服务器成功,返回结果:" + res);
            }

        } catch (Exception e) {
            log.error("发送请求异常:" + e);
            throw new AppException("测试异常");
        }

        String[] respMsg = res.split(RESP_MSG_PARAM_SEPARATOR);

        String merchantId1 = respMsg[0].substring(RESP_MSG_PARAM_PREFIX_MERCHANT_ID.length());
        String respXmlEnc = respMsg[1].substring(RESP_MSG_PARAM_PREFIX_XML_ENC.length());
        String respKeyEnc = respMsg[2].substring(RESP_MSG_PARAM_PREFIX_KEY_ENC.length());
        String respSign = respMsg[3].substring(RESP_MSG_PARAM_PREFIX_SIGN.length());

        byte respKey[] = decryptKey(respKeyEnc, rsaHelper);

        String respXml = decrytXml(respXmlEnc, respKey);

        System.out.println("返回报文merchantId:" + merchantId1);
        System.out.println("返回报文XML:" + respXml);
        System.out.println("返回报文签名:" + respSign);

        Assert.assertTrue("返回报文校验失败", verifyMsgSign(respXml, respSign, rsaHelper));

        return respXml;
    }

    public byte[] decryptKey(String hexkey, RSAHelper rsaHelper) {
        byte[] key = null;
        byte[] enc_key = StringUtil.hexStringToBytes(hexkey);

        try {
            key = rsaHelper.decryptRSA(enc_key, false, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return key;
    }

    public String encryptXml(String xml, byte[] key) {

        String enc_xml = CryptoUtils.desEncryptToHex(xml, key);
        System.out.println("xml密文：" + enc_xml);

        return enc_xml;
    }

    public String decrytXml(String xml_enc, byte[] key) {
        String xml = CryptoUtils.desDecryptFromHex(xml_enc, key);
        return xml;
    }

    public String encryptKey(byte[] key, RSAHelper rsaHelper) {

        byte[] enc_key = null;
        try {
            enc_key = rsaHelper.encryptRSA(key, false, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String hex_key = StringUtil.bytesToHexString(enc_key);
        System.out.println("密钥密文:" + hex_key);

        return hex_key;
    }

    public boolean verifyMsgSign(String xml, String sign, RSAHelper rsaHelper) {
        byte[] bsign = StringUtil.hexStringToBytes(sign);

        boolean ret = false;
        try {
            ret = rsaHelper.verifyRSA(xml.getBytes("UTF-8"), bsign, false, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }
}
