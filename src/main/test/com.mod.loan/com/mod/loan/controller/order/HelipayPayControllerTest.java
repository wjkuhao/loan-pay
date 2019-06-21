package com.mod.loan.controller.order;

import com.alibaba.fastjson.JSONObject;
import com.mod.loan.config.Constant;
import com.mod.loan.model.Order;
import com.mod.loan.model.OrderPay;
import com.mod.loan.util.TimeUtils;
import com.mod.loan.util.heliutil.HeliPayUtils;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;

public class HelipayPayControllerTest {

    private static final String tempDir = System.getProperty("java.io.tmpdir");

    private final static String hlb_id = "C1800626358";
    private final static Long uid = 8049878L;
    private final static Long orderId = 6065719L;
    private static String amount = "0.12";
    private final static String name = "帅学莉";
    private final static String bankCardNo = "6217003810050893257";
    private final static String cardNo = "511102199311266122";
    private final static String bankCardCode = "CCB";
    private final static String phone = "15983376982";
    private final static String privateRsaKey="MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBANZIARIGFaEkQVP3zjylP8403COQjtz0KpPjYVv3tulM1EnC1yWBbwYdVp7d76VEEWOvVSqf+2xpiKd9R7oRFU539nqVNCO9bmx7N6LYYmsXot1EpdfwS2vmZw0wMXCl8XQUEFIA86irLCIjHRoAKX/APT/wOqeCjtiDLmdjbG4jAgMBAAECgYEAlDBppRhWHwHet/45P5GcpbPCxkmzszScuXfXne2bLo72hShc99Aigt0JSYb8s5N2XzUjNf47t4bofcRTNWeZDk13VAFXqO2OkWOwTF5VXmNFLeatWJ1LS7DtmHiCRU94Y3CIEh1Wd0osNR42IrxZzO3pXgG3x9JAf+01TPw7CrkCQQD7tIw6/29cdcXvRJuzT6rFlVHd0pFcTxPCucc4Ub/ZWGmnkeIpfaIt/O75u/jANbVExezDhnunVQv79mihneANAkEA2e/84y4fbM6zWw2o15RU38LVOIB2sVXE3PU7IlbqvFrY57t2r2TfmLYrzKqAkBreyisig33pBmIK2owo+bbK7wJAaY99pByOOouV+FOgrOHDb9vAmrP9jGYJlofNOhxmxiMjJEi1PctXocvK+WdQ2mNr4yzSr+mNoE61mPTtSqJoXQJAbzwojSO7cmPF85FzPiU9dMAS6DXZXwXx5v1b41kNknmkABMUrs0DuwQwMtRLrkGouPNvOEIJr/JWq9miCGKeDQJATnXaVI6S+gghGYjZL1hmOiuNYT1N7PYgjaA4h3pFeBc8FwQ177mieZ5CCOahUqPjwUHzG8tGlH/un80cA+JFkQ==";
    private final static String HELIPAY_TRANSFER_URL = "http://transfer.trx.helipay.com/trx/transfer/interface.action";

    public static void helibaoPay() {
        try {
            String serials_no = String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5),
                    uid);
            if ("dev".equals(Constant.ENVIROMENT)) {
                amount = "0.12";
            }
            LinkedHashMap<String, String> sPara = new LinkedHashMap<String, String>();
            sPara.put("P1_bizType", "Transfer");// 请求类型
            sPara.put("P2_orderId", serials_no);// 请求编号
            sPara.put("P3_customerNumber", hlb_id);// 商户编号
            sPara.put("P4_amount", amount);// 订单金额


            sPara.put("P5_bankCode", "ABC");// 银行编码
            sPara.put("P6_bankAccountNo", bankCardNo);// 银行账户号
            sPara.put("P7_bankAccountName", name);// 银行账户名
            sPara.put("P8_biz", "B2C");// 业务，b2b,b2c等
            sPara.put("P9_bankUnionCode", "");// 联行号
            sPara.put("P10_feeType", "PAYER");// 手续费收取方（RECEIVER）
            sPara.put("P11_urgency", "true");// true加急
            sPara.put("P12_summary", "");// 打款备注

            JSONObject result = HeliPayUtils.requestRSA(HELIPAY_TRANSFER_URL, sPara, privateRsaKey);
            System.out.println(result);
            OrderPay orderPay = new OrderPay();
            orderPay.setPayNo(serials_no);
            orderPay.setUid(uid);
            orderPay.setOrderId(orderId);
            orderPay.setPayMoney(new BigDecimal(amount));
            orderPay.setBank("农业银行");
            orderPay.setBankNo(cardNo);
            orderPay.setCreateTime(new Date());

            if ("0000".equals(result.getString("rt2_retCode"))) {
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(1);// 受理成功,插入打款流水，不改变订单状态
            } else {

                String msg = result.getString("rt3_retMsg");
                orderPay.setRemark(msg);
                orderPay.setUpdateTime(new Date());
                orderPay.setPayStatus(2);
                Order record = new Order();
                record.setId(orderId);
                record.setStatus(23);
                //合利宝商户给用户放款失败,发送短信提醒
                if (msg.contains("商户账户余额不足")) {
                    System.out.println("商户账户余额不足");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
    }

    public static void main(String[] args) {
        helibaoPay();
    }
}