package com.mod.loan.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.mod.loan.config.Constant;
import com.mod.loan.model.Merchant;
import com.mod.loan.model.Order;
import com.mod.loan.model.User;
import com.mod.loan.model.UserBank;
import com.mod.loan.service.HelipayEntrustedPayService;
import com.mod.loan.service.MerchantService;
import com.mod.loan.service.UserBankService;
import com.mod.loan.service.UserService;
import com.mod.loan.util.heliutil.*;
import com.mod.loan.util.heliutil.vo.*;
import com.mod.loan.util.oss.OssUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class HelipayEntrustedPayServiceImpl implements HelipayEntrustedPayService {

    private static final Logger logger = LoggerFactory.getLogger(HelipayEntrustedPayServiceImpl.class);

    @Autowired
    private UserService userService;
    @Autowired
    private UserBankService userBankService;
    @Autowired
    private MerchantService merchantService;

    /**
     * 绑定用户到委托代付商户端
     */
    @Override
    public MerchantUserUploadResVo bindUserCard(Long uid, String merchantAlias) {
        User user = userService.selectByPrimaryKey(uid);
        UserBank userBank = userBankService.selectUserCurrentBankCard(uid);
        Merchant merchant = merchantService.findMerchantByAlias(merchantAlias);
        return bindUserCard(user, userBank, merchant);
    }

    /**
     * 绑定用户到委托代付商户端
     */
    private MerchantUserUploadResVo bindUserCard(User user, UserBank userBank, Merchant merchant) {
        MerchantUserUploadResVo uploadResVo = new MerchantUserUploadResVo();
        try {
            //step 1.用户注册
            MerchantUserResVo userResVo = userRegister(user, userBank, merchant);
            if (userResVo != null && "0000".equals(userResVo.getRt2_retCode())) {
                //step 2.用户资料上传
                uploadResVo = userUpload(user, merchant, userResVo.getRt6_userId(), HeliPayUtils.getOrderId(user.getId().toString()));
                if (uploadResVo != null && "0000".equals(uploadResVo.getRt2_retCode())) {
                    //将客户cuid存入数据库
                    userBank.setHlbEntrustedCuid(userResVo.getRt6_userId());
                    userBankService.updateByPrimaryKey(userBank);
                    logger.info("bindUserCard用户注册成功:{}", user.getId().toString());
                }
            }
        } catch (Exception e) {
            uploadResVo.setRt2_retCode("-1");
            uploadResVo.setRt3_retMsg("bindUserCard失败:" + e.getMessage());
            logger.error("bindUserCard用户注册失败:{},{}", user.getId().toString(), e.getMessage());
        }
        return uploadResVo;
    }

    /**
     * 委托代付绑定用户并付款
     */
    @Override
    public OrderResVo bindUserCardPay(String payNo, User user, UserBank userBank, Order order, Merchant merchant) {
        OrderResVo orderResVo = new OrderResVo();
        try {
            String amount = order.getActualMoney().toString();
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "0.1";
            }
            //step 1.注册用户到商户端
            MerchantUserUploadResVo resVo = bindUserCard(user, userBank, merchant);
            if (resVo != null && "0000".equals(resVo.getRt2_retCode())) {
                //step 2.创建委托代付订单
                orderResVo = entrustedPay(payNo, amount, user, userBank, merchant);
            }
        } catch (Exception e) {
            orderResVo.setRt2_retCode("-1");
            orderResVo.setRt3_retMsg("委托代付绑定用户并付款错误:" + e.getMessage());
            logger.error("委托代付绑定用户并付款错误:", e);
        }
        return orderResVo;
    }

    /**
     * 商户用户注册
     */
    public MerchantUserResVo userRegister(User user, UserBank userBank, Merchant merchant) {
        MerchantUserResVo merchantUserResVo = new MerchantUserResVo();
        try {
            MerchantUserVo userVo = new MerchantUserVo();
            //交易类型
            userVo.setP1_bizType("MerchantUserRegister");
            //商户编号
            userVo.setP2_customerNumber(merchant.getHlb_id());
            //商户订单号
            userVo.setP3_orderId(HeliPayUtils.getOrderId(user.getId().toString()));
            //姓名
            userVo.setP4_legalPerson(user.getUserName());
            //身份证号
            userVo.setP5_legalPersonID(user.getUserCertNo());
            //手机号
            userVo.setP6_mobile(userBank.getCardPhone());
            //对公对私
            userVo.setP7_business(HelipayConstant.BUSSINESS);
            //时间戳
            userVo.setP8_timestamp(HeliPayUtils.getTimestamp());
            //信息域
            userVo.setP9_ext(new JSONObject().toJSONString());

            //信息域加密
            if (StringUtils.isNotBlank(userVo.getP5_legalPersonID())) {
                userVo.setP5_legalPersonID(Des3Encryption.encode(HelipayConstant.deskey_key, userVo.getP5_legalPersonID()));
            }
            if (StringUtils.isNotBlank(userVo.getP6_mobile())) {
                userVo.setP6_mobile(Des3Encryption.encode(HelipayConstant.deskey_key, userVo.getP6_mobile()));
            }
            Map<String, String> map = HelipayBeanUtils.convertBean(userVo, new LinkedHashMap());
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            logger.info("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(merchant.getHlbEntrustedPrivateKey()));
            logger.info("签名串：" + sign);
            map.put("sign", sign);
            logger.info("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, Constant.HELIPAY_ENTRUSTED_URL);
            //响应结果：{"rt6_userId":"U1702451476","rt2_retCode":"0000","sign":"18884a6ddb5d0a827684db30d2de172b","rt1_bizType":"MerchantUserRegister","rt5_orderId":"p201905301446111","rt4_customerNumber":"C1800685715","rt3_retMsg":"请求成功","rt7_userStatus":"INIT"}
            logger.info("响应结果：" + result);
            merchantUserResVo = JSONObject.parseObject(result, MerchantUserResVo.class);
            if ("0000".equals(merchantUserResVo.getRt2_retCode())) {
                logger.info("用户注册成功:" + merchantUserResVo.getRt3_retMsg());
            } else {
                logger.info("用户注册失败:" + merchantUserResVo.getRt3_retMsg());
            }
        } catch (Exception e) {
            logger.error("合利宝委托代付用户注册失败:", e);
            merchantUserResVo.setRt2_retCode("-1");
            merchantUserResVo.setRt3_retMsg("合利宝委托代付用户注册失败:" + e.getMessage());
        }
        return merchantUserResVo;
    }

    /**
     * 用户资料上传
     */
    private MerchantUserUploadResVo userUpload(User user, Merchant merchant, String userRegId, String orderId) {
        //step 1.身份证正面
        MerchantUserUploadResVo resVo = userFileUpload(user.getImgCertFront(), userRegId, "FRONT_OF_ID_CARD", merchant, orderId);
        if ("0000".equals(resVo.getRt2_retCode())) {
            //step 2.身份证反面
            resVo = userFileUpload(user.getImgCertBack(), userRegId, "BACK_OF_ID_CARD", merchant, orderId);
        }
        return resVo;
    }

    /**
     * 用户资料上传
     */
    private MerchantUserUploadResVo userFileUpload(String certFile, String userRegId, String certType, Merchant merchant, String orderId) {
        MerchantUserUploadResVo uploadResVo = new MerchantUserUploadResVo();
        try {
            MerchantUserUploadVo userVo = new MerchantUserUploadVo();
            //交易类型
            userVo.setP1_bizType("UploadCredential");
            //商户编号
            userVo.setP2_customerNumber(merchant.getHlb_id());
            //商户订单号
            userVo.setP3_orderId(orderId);
            //用户编号
            userVo.setP4_userId(userRegId);
            //时间戳
            userVo.setP5_timestamp(HeliPayUtils.getTimestamp());
            //证件类型
            userVo.setP6_credentialType(certType);
            String fileName = certFile.substring(certFile.lastIndexOf("/") + 1);
            MultipartFile file = new MockMultipartFile(fileName, fileName, "", OssUtil.getCertImage(certFile));
            File tempFile = new File(HelipayConstant.tempDir, file.getOriginalFilename());
            file.transferTo(tempFile);
            // 文件签名
            try (InputStream is = new FileInputStream(tempFile)) {
                userVo.setP7_fileSign(DigestUtils.md5DigestAsHex(is));
            }
            Map<String, String> map = HelipayBeanUtils.convertBean(userVo, new LinkedHashMap());
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            logger.info("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(merchant.getHlbEntrustedPrivateKey()));
            logger.info("签名串：" + sign);
            map.put("sign", sign);
            logger.info("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, Constant.HELIPAY_ENTRUSTED_FILE_URL, tempFile);
            logger.info("响应结果：" + result);
            //{response={"rt6_userId":"","rt2_retCode":"1024","sign":"f4855210123cfaa21faf45d4e72cf25b","rt1_bizType":"UploadCredential","rt5_orderId":"p201905311018541","rt8_desc":"","rt4_customerNumber":"C1800685715","rt3_retMsg":"未找到该用户","rt7_credentialStatus":""}, statusCode=200}
            uploadResVo = JSONObject.parseObject(result, MerchantUserUploadResVo.class);
            String assemblyRespOriSign = HelipayBeanUtils.getSigned(uploadResVo, null);
            logger.info("组装返回结果签名串：" + assemblyRespOriSign);
            String responseSign = uploadResVo.getSign();
            logger.info("响应签名：" + responseSign);
            if ("0000".equals(uploadResVo.getRt2_retCode())) {
                logger.info("上传资料成功:" + uploadResVo.getRt3_retMsg());
            } else {
                logger.info("上传资料失败:" + uploadResVo.getRt3_retMsg());
            }
        } catch (Exception e) {
            logger.error("合利宝委托代付用户资质认证失败:", e);
            uploadResVo.setRt2_retCode("-1");
            uploadResVo.setRt3_retMsg("合利宝委托代付用户资质认证失败:" + e.getMessage());
        }
        return uploadResVo;
    }

    /**
     * 合利宝委托代付接口
     */
    @Override
    public OrderResVo entrustedPay(String payNo, String amount, User user, UserBank userBank, Merchant merchant) {
        OrderResVo orderResVo = new OrderResVo();
        try {
            OrderVo orderVo = new OrderVo();
            //交易类型
            orderVo.setP1_bizType("EntrustedLoanTransfer");
            //商户编号
            orderVo.setP2_customerNumber(merchant.getHlb_id());
            //商户订单号
            orderVo.setP3_orderId(payNo);
            //用户编号
            orderVo.setP4_userId(userBank.getHlbEntrustedCuid());
            //时间戳
            orderVo.setP5_timestamp(HeliPayUtils.getTimestamp());
            //币种
            orderVo.setP6_currency("CNY");
            //金额
            orderVo.setP7_amount(amount);
            //对公对私
            orderVo.setP8_business(HelipayConstant.BUSSINESS);
            //账户名
            orderVo.setP9_bankAccountName(user.getUserName());
            //账户号
            orderVo.setP10_bankAccountNo(userBank.getCardNo());
            //证件号
            orderVo.setP11_legalPersonID(user.getUserCertNo());
            //手机号
            orderVo.setP12_mobile(userBank.getCardPhone());
            //借贷记类型
            orderVo.setP13_onlineCardType("DEBIT");
            //银行编码
            orderVo.setP17_bankCode(userBank.getCardCode());
            //通知地址
            orderVo.setP19_callbackUrl("");
            //用途
            orderVo.setP20_purpose("教育");
            //分期扩展参数
            orderVo.setP21_loanConInfo(getOrderExtend(new BigDecimal(amount)).toJSONString());

            if (StringUtils.isNotBlank(orderVo.getP10_bankAccountNo())) {
                orderVo.setP10_bankAccountNo(Des3Encryption.encode(HelipayConstant.deskey_key, orderVo.getP10_bankAccountNo()));
            }
            if (StringUtils.isNotBlank(orderVo.getP11_legalPersonID())) {
                orderVo.setP11_legalPersonID(Des3Encryption.encode(HelipayConstant.deskey_key, orderVo.getP11_legalPersonID()));
            }
            if (StringUtils.isNotBlank(orderVo.getP12_mobile())) {
                orderVo.setP12_mobile(Des3Encryption.encode(HelipayConstant.deskey_key, orderVo.getP12_mobile()));
            }
            if (StringUtils.isNotBlank(orderVo.getP14_year())) {
                orderVo.setP14_year(Des3Encryption.encode(HelipayConstant.deskey_key, orderVo.getP14_year()));
            }
            if (StringUtils.isNotBlank(orderVo.getP15_month())) {
                orderVo.setP15_month(Des3Encryption.encode(HelipayConstant.deskey_key, orderVo.getP15_month()));
            }
            if (StringUtils.isNotBlank(orderVo.getP16_cvv2())) {
                orderVo.setP16_cvv2(Des3Encryption.encode(HelipayConstant.deskey_key, orderVo.getP16_cvv2()));
            }
            Map<String, String> map = HelipayBeanUtils.convertBean(orderVo, new LinkedHashMap());
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            logger.info("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(merchant.getHlbEntrustedPrivateKey()));
            logger.info("签名串：" + sign);
            map.put("sign", sign);
            logger.info("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, Constant.HELIPAY_ENTRUSTED_URL);
            logger.info("响应结果：" + result);
            //响应结果：{"rt6_userId":"U1702451476","rt2_retCode":"0000","rt7_serialNumber":"1235976","rt9_timestamp":"2019-05-31_14:45:10.059","rt8_orderStatus":"RECEIVE","sign":"88123913e88b4ed64fce8b566b6e3f60","rt1_bizType":"EntrustedLoanTransfer","rt5_orderId":"p201905311444581","rt4_customerNumber":"C1800685715","rt3_retMsg":"请求成功"}
            orderResVo = JSONObject.parseObject(result, OrderResVo.class);
            if ("0000".equals(orderResVo.getRt2_retCode())) {
                logger.info("委托代付成功:" + orderResVo.getRt3_retMsg());
            } else {
                logger.info("委托代付失败:" + orderResVo.getRt3_retMsg());
            }
//            String assemblyRespOriSign = HelipayBeanUtils.getSigned(resVo, null);
//            logger.info("组装返回结果签名串：" + assemblyRespOriSign);
//            String responseSign = resVo.getSign();
//            logger.info("响应签名：" + responseSign);
//            String checkSign = Disguiser.disguiseMD5(assemblyRespOriSign.trim() + HelipayConstant.split + HelipayConstant.signkey);
//            if (checkSign.equals(responseSign)) {
//                logger.info("委托代付成功:" + resVo.getRt3_retMsg());
//            }
        } catch (Exception e) {
            logger.error("合利宝委托代付打款失败", e);
            orderResVo.setRt2_retCode("-1");
            orderResVo.setRt3_retMsg(e.getMessage());
        }
        return orderResVo;

    }

    /**
     * 委托代付订单查询
     */
    @Override
    public OrderQueryResVo entrustedPayQuery(String payNo, UserBank userBank, Merchant merchant) {
        OrderQueryResVo orderQueryResVo = new OrderQueryResVo();
        try {
            OrderQueryVo orderVo = new OrderQueryVo();
            //交易类型
            orderVo.setP1_bizType("EntrustedLoanTransferQuery");
            //商户编号
            orderVo.setP2_customerNumber(merchant.getHlb_id());
            //商户订单号
            orderVo.setP3_orderId(payNo);
            //用户编号
            orderVo.setP4_userId(userBank.getHlbEntrustedCuid());
            //时间戳
            orderVo.setP5_timestamp(HeliPayUtils.getTimestamp());

            Map<String, String> map = HelipayBeanUtils.convertBean(orderVo, new LinkedHashMap());
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            logger.info("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(merchant.getHlbEntrustedPrivateKey()));
            logger.info("签名串：" + sign);
            map.put("sign", sign);
            logger.info("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, HelipayConstant.REQUEST_URL);
            logger.info("响应结果：" + result);
            orderQueryResVo = JSONObject.parseObject(result, OrderQueryResVo.class);
            if ("0000".equals(orderQueryResVo.getRt2_retCode())) {
                logger.info("委托代付订单查询成功:" + orderQueryResVo.getRt3_retMsg() + "," + orderQueryResVo.getRt9_desc());
            } else {
                logger.info("委托代付订单查询失败:" + orderQueryResVo.getRt3_retMsg() + "," + orderQueryResVo.getRt9_desc());
            }
        } catch (Exception e) {
            logger.error("合利宝委托代付结果查询失败", e);
            orderQueryResVo.setRt2_retCode("-1");
            orderQueryResVo.setRt3_retMsg("合利宝委托代付结果查询失败:" + e.getMessage());
        }
        return orderQueryResVo;
    }

    /**
     * 计算每期费用
     */
    private JSONObject getOrderExtend(BigDecimal borrowMoney) {
        //借款时间
        int loanTime = 90;
        //借款利率数值
        int loanInterestRate = 18;
        //借款利率
        int periodization = 3;
        int periodizationDays = loanTime / periodization;
        //分期金额计算公式 =（借款金额+借款金额×借款年利率÷360×借款天数）÷分期数  (borrowMoney + borrowMoney*loanInterestRate/360*loanTime)/periodization;
        BigDecimal periodizationFee = borrowMoney.add(borrowMoney.multiply(new BigDecimal(loanInterestRate)).divide(new BigDecimal(100)).divide(new BigDecimal(360)).multiply(new BigDecimal(loanTime))).divide(new BigDecimal(periodization), 2, BigDecimal.ROUND_HALF_UP);

        JSONObject data = new JSONObject();
        //借款时间
        data.put("loanTime", loanTime);
        //借款时间单位
        data.put("loanTimeUnit", "D");
        //借款利率
        data.put("loanInterestRate", loanInterestRate);
        //周期化
        data.put("periodization", periodization);
        //周期天数
        data.put("periodizationDays", periodizationDays);
        //每期金额
        data.put("periodizationFee", periodizationFee);
        return data;

    }
}

