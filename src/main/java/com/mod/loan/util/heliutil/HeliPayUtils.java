package com.mod.loan.util.heliutil;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mod.loan.util.MD5;
import com.mod.loan.util.TimeUtils;
import org.joda.time.DateTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;

public class HeliPayUtils {

    public static JSONObject requestMD5(String requrl, LinkedHashMap<String, String> params, String md5_key) {
        String sign = getSignMD5(params, md5_key);
        params.put("sign", sign);
        String resp = HttpClientService.getHttpResp(params, requrl);
        JSONObject result = JSON.parseObject(resp);
        return result;
    }

    public static JSONObject requestRSA(String requrl, LinkedHashMap<String, String> params, String rsa_key) {
        String sign = getSignRSA(params, rsa_key);
        params.put("sign", sign);
        String resp = HttpClientService.getHttpResp(params, requrl);
        JSONObject result = JSON.parseObject(resp);
        return result;
    }

    private static String getSignRSA(LinkedHashMap<String, String> params, String rsa_key) {
        String content = generateParams(params);
        try {
            return RSA.sign(rsa_key, content);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String getSignMD5(LinkedHashMap<String, String> params, String md5_key) {
        String content = generateParams(params) + "&" + md5_key;
        return MD5.toMD5(content.toString());
    }

    private static String generateParams(LinkedHashMap<String, String> params) {
        StringBuffer content = new StringBuffer();
        List<String> keys = new ArrayList<String>(params.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);
            content.append("&" + value);
        }
        return content.toString();
    }

    /**
     * 委托代付接口时间戳
     */
    public static String getTimestamp() {
        SimpleDateFormat STRING_FORMAT_TIMESTAMP_TEST = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.sss");
        String dateString1 = STRING_FORMAT_TIMESTAMP_TEST.format(new Date());
        return dateString1;
    }

    /**
     * 委托代付接口获取订单ID
     */
    public static String getOrderId(String uid) {
        return String.format("%s%s%s", "p", new DateTime().toString(TimeUtils.dateformat5), uid);
    }


}
