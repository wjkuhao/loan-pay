package com.mod.loan.controller.order;

import com.alibaba.fastjson.JSONObject;
import com.mod.loan.config.Constant;
import com.mod.loan.util.TimeUtils;
import com.mod.loan.util.heliutil.*;
import com.mod.loan.util.heliutil.vo.*;
import com.mod.loan.util.oss.OssUtil;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class HelipayEntrustedPayControllerTest {

    private static final String tempDir = System.getProperty("java.io.tmpdir");

    private final static String hlb_id = "C1800685715";
    private final static String uid = "1";
    private final static String amount = "10";
    private final static String name = "童官浦";
    private final static String bankCardNo = "6228480329322167670";
    private final static String cardNo = "352225199307145556";
    private final static String bankCardCode = "ABC";
    private final static String phone = "18557530599";
    private final static String bussiness = "B2C";
    private final static String certFront = "2019/0423/b055c62f34fe4c928e0e96b002c602e1.jpg";
    private final static String certBack = "2019/0423/c6ec6c9ceddd49a491a5607a818f39ca.jpg";
    private final static String userRegId = "U1702451476";

    /**
     * 商户用户注册
     */
    public static MerchantUserResVo user_register() {
        MerchantUserResVo merchantUserResVo = null;
        try {
            MerchantUserVo userVo = new MerchantUserVo();
            //交易类型
            userVo.setP1_bizType("MerchantUserRegister");
            //商户编号
            userVo.setP2_customerNumber(hlb_id);
            //商户订单号
            userVo.setP3_orderId(String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5), uid));
            //姓名
            userVo.setP4_legalPerson(name);
            //身份证号
            userVo.setP5_legalPersonID(cardNo);
            //手机号
            userVo.setP6_mobile(phone);
            //对公对私
            userVo.setP7_business(bussiness);
            //时间戳
            userVo.setP8_timestamp(getTimestamp());
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
            System.out.println("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(HelipayConstant.signKey_private));
            System.out.println("签名串：" + sign);
            map.put("sign", sign);
            System.out.println("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, HelipayConstant.REQUEST_URL);
            //响应结果：{"rt6_userId":"U1702451476","rt2_retCode":"0000","sign":"18884a6ddb5d0a827684db30d2de172b","rt1_bizType":"MerchantUserRegister","rt5_orderId":"p201905301446111","rt4_customerNumber":"C1800685715","rt3_retMsg":"请求成功","rt7_userStatus":"INIT"}
            System.out.println("响应结果：" + result);
            merchantUserResVo = JSONObject.parseObject(result, MerchantUserResVo.class);
            if ("0000".equals(merchantUserResVo.getRt2_retCode())) {
                System.out.println("用户注册成功:" + merchantUserResVo.getRt3_retMsg());
            } else {
                System.out.println("用户注册失败:" + merchantUserResVo.getRt3_retMsg());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return merchantUserResVo;
    }

    /**
     * 用户查询
     */
    public static MerchantUserQueryResVo user_query(String userRegId) {
        MerchantUserQueryResVo resVo = null;
        try {
            MerchantUserQueryVo userVo = new MerchantUserQueryVo();
            //交易类型
            userVo.setP1_bizType("MerchantUserQuery");
            //商户编号
            userVo.setP2_customerNumber(hlb_id);
            //商户订单号
            userVo.setP3_orderId(getOrderId(uid));
            //用户编号
            userVo.setP4_userId(userRegId);
            //时间戳
            userVo.setP5_timestamp(getTimestamp());

            Map<String, String> map = HelipayBeanUtils.convertBean(userVo, new LinkedHashMap());
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            System.out.println("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(HelipayConstant.signKey_private));
            ;
            System.out.println("签名串：" + sign);
            map.put("sign", sign);
            System.out.println("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, HelipayConstant.REQUEST_URL);
            System.out.println("响应结果：" + result);
            //{"rt6_userId":"U1702451476","rt2_retCode":"0000","sign":"27f48cf80592a51880b6776e27dc2446","rt1_bizType":"MerchantUserQuery","rt5_orderId":"p201905301457021","rt8_desc":"待审核","rt4_customerNumber":"C1800685715","rt3_retMsg":"请求成功","rt7_userStatus":"INIT"}
            resVo = JSONObject.parseObject(result, MerchantUserQueryResVo.class);
            if ("0000".equals(resVo.getRt2_retCode())) {
                System.out.println("用户查询成功:" + resVo.getRt3_retMsg());
            } else {
                System.out.println("用户查询失败:" + resVo.getRt3_retMsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resVo;
    }

    /**
     * 用资料上传
     */
    public static MerchantUserUploadResVo user_upload(String certFile, String certType, String userRegId) {
        MerchantUserUploadResVo uploadResVo = null;
        try {
            MerchantUserUploadVo userVo = new MerchantUserUploadVo();
            //交易类型
            userVo.setP1_bizType("UploadCredential");
            //商户编号
            userVo.setP2_customerNumber(hlb_id);
            //商户订单号
            userVo.setP3_orderId(getOrderId(uid));
            //用户编号
            userVo.setP4_userId(userRegId);
            //时间戳
            userVo.setP5_timestamp(getTimestamp());
            //证件类型
            userVo.setP6_credentialType(certType);

            String fileName = certFile.substring(certFile.lastIndexOf("/") + 1);
            MultipartFile file = new MockMultipartFile(fileName, fileName, "", OssUtil.getCertImage(certFile));
            File tempFile = new File(tempDir, file.getOriginalFilename());
            file.transferTo(tempFile);
            // 文件签名
            try (InputStream is = new FileInputStream(tempFile)) {
                userVo.setP7_fileSign(DigestUtils.md5DigestAsHex(is));
            }
            Map<String, String> map = HelipayBeanUtils.convertBean(userVo, new LinkedHashMap());
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            System.out.println("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(HelipayConstant.signKey_private));
            System.out.println("签名串：" + sign);
            map.put("sign", sign);
            System.out.println("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, HelipayConstant.REQUEST_FILEURL, tempFile);
            System.out.println("响应结果：" + result);
            //{response={"rt6_userId":"","rt2_retCode":"1024","sign":"f4855210123cfaa21faf45d4e72cf25b","rt1_bizType":"UploadCredential","rt5_orderId":"p201905311018541","rt8_desc":"","rt4_customerNumber":"C1800685715","rt3_retMsg":"未找到该用户","rt7_credentialStatus":""}, statusCode=200}
            uploadResVo = JSONObject.parseObject(result, MerchantUserUploadResVo.class);
            String assemblyRespOriSign = HelipayBeanUtils.getSigned(uploadResVo, null);
            System.out.println("组装返回结果签名串：" + assemblyRespOriSign);
            String responseSign = uploadResVo.getSign();
            System.out.println("响应签名：" + responseSign);
            if ("0000".equals(uploadResVo.getRt2_retCode())) {
                System.out.println("上传资料成功:" + uploadResVo.getRt3_retMsg());
            } else {
                System.out.println("上传资料失败:" + uploadResVo.getRt3_retMsg());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uploadResVo;
    }

    /**
     * 用户资质查询
     */
    public static void user_upload_query(String certType) {
        try {
            MerchantUserUploadVo userVo = new MerchantUserUploadVo();
            //交易类型
            userVo.setP1_bizType("UploadCredentialQuery");
            //商户编号
            userVo.setP2_customerNumber(hlb_id);
            //商户订单号
            userVo.setP3_orderId(getOrderId(uid));
            //用户编号
            userVo.setP4_userId(userRegId);
            //时间戳
            userVo.setP5_timestamp(getTimestamp());
            //资质类型
            userVo.setP6_credentialType(certType);

            Map<String, String> map = HelipayBeanUtils.convertBean(userVo, new LinkedHashMap(), "P7_fileSign");
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            System.out.println("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(HelipayConstant.signKey_private));
            System.out.println("签名串：" + sign);
            map.put("sign", sign);
            System.out.println("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, HelipayConstant.REQUEST_URL);
            System.out.println("响应结果：" + result);
            MerchantUserUploadResVo resVo = JSONObject.parseObject(result, MerchantUserUploadResVo.class);
            if ("0000".equals(resVo.getRt2_retCode())) {
                System.out.println(certType + " 资质查询成功," + resVo.getRt8_desc());
            } else {
                System.out.println(certType + " 资质查询失败" + resVo.getRt8_desc());
            }
                /*String assemblyRespOriSign = HelipayBeanUtils.getSigned(resVo, null);
                System.out.println("组装返回结果签名串：" + assemblyRespOriSign);
                String responseSign = resVo.getSign();
                System.out.println("响应签名：" + responseSign);
                String checkSign = Disguiser.disguiseMD5(assemblyRespOriSign.trim()+HelipayConstant.split+HelipayConstant.signkey);
                if (checkSign.equals(responseSign)) {

                } else {
                }*/

        } catch (Exception e) {
            e.printStackTrace();

        }
    }


    /**
     * 合利宝委托代付接口测试
     */
    public static OrderResVo proxy_pay(String userRegId) {
        OrderResVo orderResVo = null;
        try {
            OrderVo orderVo = new OrderVo();
            //交易类型
            orderVo.setP1_bizType("EntrustedLoanTransfer");
            //商户编号
            orderVo.setP2_customerNumber(hlb_id);
            //商户订单号
            orderVo.setP3_orderId(String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5), uid));
            //用户编号
            orderVo.setP4_userId(userRegId);
            //时间戳
            orderVo.setP5_timestamp(getTimestamp());
            //币种
            orderVo.setP6_currency("CNY");
            //金额
            String money = "dev".equals(Constant.ENVIROMENT) ? "0.11" : amount;
            orderVo.setP7_amount(money);
            //对公对私
            orderVo.setP8_business(bussiness);
            //账户名
            orderVo.setP9_bankAccountName(name);
            //账户号
            orderVo.setP10_bankAccountNo(bankCardNo);
            //证件号
            orderVo.setP11_legalPersonID(cardNo);
            //手机号
            orderVo.setP12_mobile(phone);
            //借贷记类型
            orderVo.setP13_onlineCardType("DEBIT");
            //银行编码
            orderVo.setP17_bankCode(bankCardCode);
            //通知地址
            orderVo.setP19_callbackUrl("");
            //用途
            orderVo.setP20_purpose("test");
            //分期扩展参数
            orderVo.setP21_loanConInfo(getOrderExtend(new BigDecimal(money)).toJSONString());

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
            System.out.println("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(HelipayConstant.signKey_private));
            System.out.println("签名串：" + sign);
            map.put("sign", sign);
            System.out.println("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, HelipayConstant.REQUEST_URL);
            System.out.println("响应结果：" + result);
            //响应结果：{"rt6_userId":"U1702451476","rt2_retCode":"0000","rt7_serialNumber":"1235976","rt9_timestamp":"2019-05-31_14:45:10.059","rt8_orderStatus":"RECEIVE","sign":"88123913e88b4ed64fce8b566b6e3f60","rt1_bizType":"EntrustedLoanTransfer","rt5_orderId":"p201905311444581","rt4_customerNumber":"C1800685715","rt3_retMsg":"请求成功"}
            orderResVo = JSONObject.parseObject(result, OrderResVo.class);
            if ("0000".equals(orderResVo.getRt2_retCode())) {
                System.out.println("委托代付成功:" + orderResVo.getRt3_retMsg());
            } else {
                System.out.println("委托代付失败:" + orderResVo.getRt3_retMsg());
            }
//            String assemblyRespOriSign = HelipayBeanUtils.getSigned(resVo, null);
//            System.out.println("组装返回结果签名串：" + assemblyRespOriSign);
//            String responseSign = resVo.getSign();
//            System.out.println("响应签名：" + responseSign);
//            String checkSign = Disguiser.disguiseMD5(assemblyRespOriSign.trim() + HelipayConstant.split + HelipayConstant.signkey);
//            if (checkSign.equals(responseSign)) {
//                System.out.println("委托代付成功:" + resVo.getRt3_retMsg());
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderResVo;

    }

    /**
     * 委托代付订单查询
     */
    public static OrderQueryResVo proxy_pay_query(String userRegId, String orderId) {
        OrderQueryResVo orderQueryResVo = null;
        try {
            OrderQueryVo orderVo = new OrderQueryVo();
            //交易类型
            orderVo.setP1_bizType("EntrustedLoanTransferQuery");
            //商户编号
            orderVo.setP2_customerNumber(hlb_id);
            //商户订单号
            orderVo.setP3_orderId(orderId);
            //用户编号
            orderVo.setP4_userId(userRegId);
            //时间戳
            orderVo.setP5_timestamp(getTimestamp());

            Map<String, String> map = HelipayBeanUtils.convertBean(orderVo, new LinkedHashMap());
            String oriMessage = HelipayBeanUtils.getSigned(map, null);
            System.out.println("签名原文串：" + oriMessage);
            String sign = RSA.sign(oriMessage.trim(), RSA.getPrivateKey(HelipayConstant.signKey_private));
            System.out.println("签名串：" + sign);
            map.put("sign", sign);
            System.out.println("发送参数：" + map);
            String result = HttpClientService.getHttpResp(map, HelipayConstant.REQUEST_URL);
            System.out.println("响应结果：" + result);
            orderQueryResVo = JSONObject.parseObject(result, OrderQueryResVo.class);
            if ("0000".equals(orderQueryResVo.getRt2_retCode())) {
                System.out.println("委托代付订单查询成功:" + orderQueryResVo.getRt3_retMsg() + "," + orderQueryResVo.getRt9_desc());
            } else {
                System.out.println("委托代付订单查询失败:" + orderQueryResVo.getRt3_retMsg() + "," + orderQueryResVo.getRt9_desc());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orderQueryResVo;
    }

    public static String getTimestamp() {
        SimpleDateFormat STRING_FORMAT_TIMESTAMP_TEST = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.sss");
        String dateString1 = STRING_FORMAT_TIMESTAMP_TEST.format(new Date());
        return dateString1;
    }

    public static String getOrderId(String uid) {
        return String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5), uid);
    }

    /**
     * 计算每期费用
     */
    public static JSONObject getOrderExtend(BigDecimal borrowMoney) {
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

    /**
     * 全流程测试
     */
    public static void process() {
        //step 1.用户注册
        MerchantUserResVo userResVo = user_register();
        if (userResVo == null || !"0000".equals(userResVo.getRt2_retCode())) {
            System.out.println("用户注册失败");
            return;
        }
        //step 2.用户资料上传
        MerchantUserUploadResVo uploadResVo = user_upload(certFront, "FRONT_OF_ID_CARD", userResVo.getRt6_userId());
        if (uploadResVo == null || !"0000".equals(uploadResVo.getRt2_retCode())) {
            System.out.println("用户身份证正面上传失败");
            return;
        }
        uploadResVo = user_upload(certBack, "BACK_OF_ID_CARD", userResVo.getRt6_userId());
        if (uploadResVo == null || !"0000".equals(uploadResVo.getRt2_retCode())) {
            System.out.println("用户身份证反面上传失败");
            return;
        }
        //step 3.创建委托代付订单
        OrderResVo orderResVo = proxy_pay(userResVo.getRt6_userId());
        if (orderResVo == null || !"0000".equals(orderResVo.getRt2_retCode())) {
            System.out.println("创建委托订单失败");
            return;
        }
        try {
            Thread.sleep(1000 * 5);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //step 4.订单状态查询
        OrderQueryResVo orderQueryResVo = proxy_pay_query(userResVo.getRt6_userId(), orderResVo.getRt5_orderId());
        if(orderQueryResVo == null || !"0000".equals(orderQueryResVo.getRt2_retCode())){

        }
    }

    public static void main(String[] args) {
        //商户用户注册
        user_register();
        //商户用户注册查询
        //user_query();
        //用户资料证件正面上传
        //user_upload(certFront, "FRONT_OF_ID_CARD");
        //用户资料证件反面上传
        //user_upload(certBack, "BACK_OF_ID_CARD");
        //用户资料正面上传查询
        //user_upload_query("FRONT_OF_ID_CARD");
        //用户资料反面上传查询
        //user_upload_query("BACK_OF_ID_CARD");
        //委托代付创建订单
        //proxy_pay();
        //委托代付订单查询
        //proxy_pay_query("U1702451476", "p201906051357597951902");
        //计算分期扩展参数
        //getOrderExtend(new BigDecimal(10));
        //process();
    }
}