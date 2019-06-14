package com.mod.loan.util.kuaiqianutil.common;

import com.alibaba.fastjson.JSONObject;
import com.bill99.asap.exception.CryptoException;
import com.bill99.asap.service.ICryptoService;
import com.bill99.asap.service.impl.CryptoServiceFactory;
import com.bill99.schema.asap.commons.Mpf;
import com.bill99.schema.asap.data.SealedData;
import com.bill99.schema.asap.data.UnsealedData;
import com.mod.loan.util.XmlUtils;
import com.mod.loan.util.kuaiqianutil.notice.NotifyRequest;
import com.mod.loan.util.kuaiqianutil.notice.NotifyResponse;
import com.mod.loan.util.kuaiqianutil.notice.Pay2bankNotify;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankRequest;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankResponse;
import com.mod.loan.util.kuaiqianutil.pay.Pay2bankResult;
import com.mod.loan.util.kuaiqianutil.query.Pay2bankSearchRequest;
import com.mod.loan.util.kuaiqianutil.query.Pay2bankSearchResponse;
import com.mod.loan.util.kuaiqianutil.query.Pay2bankSearchResult;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.*;

public class KuaiqianHttpUtil {
    private static final Logger logger = LoggerFactory.getLogger(KuaiqianHttpUtil.class);
    //接口版本
    private static String VERSION = "1.0";
    //策略编码，固定值 F41
    private static String fetureCode = "F41";
    //字符编码
    private static String encoding = "UTF-8";

    /**
     * 明文加密加签
     *
     * @return
     */
    public static String genPayPKIMsg(String xml, String memberCode) {
        //构建一个订单对象
        logger.info("请求明文报文 = {}", xml);
        //获取原始报文
        String originalStr = xml;
        //加签、加密
        Mpf mpf = genMpf(memberCode);
        logger.info("#[快钱放款加密开始]-merchantCode={},mpf={}", memberCode, JSONObject.toJSON(mpf));
        SealedData sealedData = null;
        try {
            ICryptoService service = CryptoServiceFactory.createCryptoService();
            sealedData = service.seal(mpf, originalStr.getBytes());
        } catch (CryptoException e) {
            logger.error("快钱明文加密加签异常，e={}", e);
        }
        logger.info("快钱明文加密数据，sealedData={}", JSONObject.toJSON(sealedData));
        Pay2bankRequest request = CCSUtil.genPayRequest(memberCode, VERSION);
        byte[] nullbyte = {};
        byte[] byteOri = sealedData.getOriginalData() == null ? nullbyte : sealedData.getOriginalData();
        byte[] byteEnc = sealedData.getEncryptedData() == null ? nullbyte : sealedData.getEncryptedData();
        byte[] byteEnv = sealedData.getDigitalEnvelope() == null ? nullbyte : sealedData.getDigitalEnvelope();
        byte[] byteSig = sealedData.getSignedData() == null ? nullbyte : sealedData.getSignedData();
        request.getRequestBody().getSealDataType().setOriginalData(PKIUtil.byte2UTF8StringWithBase64(byteOri));
        //获取加签报文
        request.getRequestBody().getSealDataType().setSignedData(PKIUtil.byte2UTF8StringWithBase64(byteSig));
        //获取加密报文
        request.getRequestBody().getSealDataType().setEncryptedData(PKIUtil.byte2UTF8StringWithBase64(byteEnc));
        //数字信封
        request.getRequestBody().getSealDataType().setDigitalEnvelope(PKIUtil.byte2UTF8StringWithBase64(byteEnv));
        //请求报文
        String requestXml = XmlUtils.convertToXml(request, encoding);
        logger.info("请求加密报文 = {}", requestXml);
        return requestXml;
    }

    public static String genPayQueryPKIMsg(String xml, String memberCode) {
        //构建一个订单对象
        logger.info("请求明文报文 = {}", xml);
        //获取原始报文
        String originalStr = xml;
        //加签、加密
        Mpf mpf = genMpf(memberCode);
        logger.info("#[快钱放款查询加密开始]-merchantCode={},mpf={}", memberCode, JSONObject.toJSON(mpf));
        SealedData sealedData = null;
        try {
            ICryptoService service = CryptoServiceFactory.createCryptoService();
            sealedData = service.seal(mpf, originalStr.getBytes());
        } catch (CryptoException e) {
            System.out.println(e);
        }
        Pay2bankSearchRequest request = CCSUtil.genPayQueryRequest(memberCode, VERSION);

        byte[] nullbyte = {};
        byte[] byteOri = sealedData.getOriginalData() == null ? nullbyte : sealedData.getOriginalData();
        byte[] byteEnc = sealedData.getEncryptedData() == null ? nullbyte : sealedData.getEncryptedData();
        byte[] byteEnv = sealedData.getDigitalEnvelope() == null ? nullbyte : sealedData.getDigitalEnvelope();
        byte[] byteSig = sealedData.getSignedData() == null ? nullbyte : sealedData.getSignedData();
        request.getSearchRequestBody().getSealDataType().setOriginalData(PKIUtil.byte2UTF8StringWithBase64(byteOri));
        //获取加签报文
        request.getSearchRequestBody().getSealDataType().setSignedData(PKIUtil.byte2UTF8StringWithBase64(byteSig));
        //获取加密报文
        request.getSearchRequestBody().getSealDataType().setEncryptedData(PKIUtil.byte2UTF8StringWithBase64(byteEnc));
        //数字信封
        request.getSearchRequestBody().getSealDataType().setDigitalEnvelope(PKIUtil.byte2UTF8StringWithBase64(byteEnv));
        //请求报文
        String requestXml = XmlUtils.convertToXml(request, encoding);
        logger.info("请求加密报文 = {}", requestXml);
        return requestXml;
    }

    public static String genPayNoticeXml(String ori, String memberCode) {
        Mpf mpf = genMpf(memberCode);
        SealedData sealedData = null;
        try {
            ICryptoService service = CryptoServiceFactory.createCryptoService();
            sealedData = service.seal(mpf, ori.getBytes());
        } catch (CryptoException e) {
            System.out.println(e);
        }
        NotifyResponse response = CCSUtil.genNoticeResponse(memberCode, VERSION);
        byte[] nullbyte = {};
        byte[] byteOri = sealedData.getOriginalData() == null ? nullbyte : sealedData.getOriginalData();
        byte[] byteEnc = sealedData.getEncryptedData() == null ? nullbyte : sealedData.getEncryptedData();
        byte[] byteEnv = sealedData.getDigitalEnvelope() == null ? nullbyte : sealedData.getDigitalEnvelope();
        byte[] byteSig = sealedData.getSignedData() == null ? nullbyte : sealedData.getSignedData();
        response.getNotifyResponseBody().getSealDataType().setOriginalData(PKIUtil.byte2UTF8StringWithBase64(byteOri));
        //获取加签报文
        response.getNotifyResponseBody().getSealDataType().setSignedData(PKIUtil.byte2UTF8StringWithBase64(byteSig));
        //获取加密报文
        response.getNotifyResponseBody().getSealDataType().setEncryptedData(PKIUtil.byte2UTF8StringWithBase64(byteEnc));
        //数字信封
        response.getNotifyResponseBody().getSealDataType().setDigitalEnvelope(PKIUtil.byte2UTF8StringWithBase64(byteEnv));
        //请求报文
        String requestXml = XmlUtils.convertToXml(response, encoding);
        logger.info("请求加密报文 = " + requestXml);
        return requestXml;
    }

    /**
     * 获取请求响应的加密数据
     *
     * @param requestXml
     * @return
     * @throws Exception
     */
    public static String invokeCSSCollection(String requestXml, String url) throws Exception {
        //初始化HttpClient
        HttpClient client = new HttpClient();
        PostMethod method = new PostMethod(url);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, null, null);
        SSLContext.setDefault(sslContext);
        //请求服务端
        InputStream in_withcode = new ByteArrayInputStream(requestXml.getBytes(encoding));
        method.setRequestBody(in_withcode);
        // url的连接等待超时时间设置
        client.getHttpConnectionManager().getParams().setConnectionTimeout(2000);
        // 读取数据超时时间设置
        client.getHttpConnectionManager().getParams().setSoTimeout(3000);
        method.setRequestEntity(new StringRequestEntity(requestXml, "text/html", "utf-8"));
        client.executeMethod(method);
        //打印服务器返回的状态
        logger.info("服务器返回的状态 = {}", method.getStatusLine());
        //打印返回的信息
        InputStream stream = method.getResponseBodyAsStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, encoding));
        StringBuffer buf = new StringBuffer();
        String line;
        while (null != (line = br.readLine())) {
            buf.append(line).append("\n");
        }
        //释放连接
        method.releaseConnection();
        return buf.toString();
    }

    /**
     * 返回的加密报文解密
     *
     * @param msg
     * @throws Exception
     */
    public static Pay2bankResult unsealMsgPay(String msg, String memberCode) throws Exception {
        Pay2bankResult pay2bankResult = new Pay2bankResult();
        SealedData sealedData = new SealedData();
        Pay2bankResponse response = XmlUtils.convertToJavaBean(msg, Pay2bankResponse.class);
        sealedData.setOriginalData(response.getResponseBody().getSealDataType().getOriginalData() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getResponseBody().getSealDataType().getOriginalData()));
        sealedData.setSignedData(response.getResponseBody().getSealDataType().getSignedData() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getResponseBody().getSealDataType().getSignedData()));
        sealedData.setEncryptedData(response.getResponseBody().getSealDataType().getEncryptedData() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getResponseBody().getSealDataType().getEncryptedData()));
        sealedData.setDigitalEnvelope(response.getResponseBody().getSealDataType().getDigitalEnvelope() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getResponseBody().getSealDataType().getDigitalEnvelope()));
        Mpf mpf = genMpf(memberCode);
        UnsealedData unsealedData = null;
        try {
            ICryptoService service = CryptoServiceFactory.createCryptoService();
            unsealedData = service.unseal(mpf, sealedData);
        } catch (CryptoException e) {
            System.out.println(e);
        }
        byte[] decryptedData = unsealedData.getDecryptedData();
        if (null != decryptedData) {
            String rtnString = PKIUtil.byte2UTF8String(decryptedData);
            logger.info("解密后返回报文 = " + rtnString);
//            Pay2bankOrder pay2bankOrder = XmlUtils.convertToJavaBean(msg, Pay2bankOrder.class);
//            pay2bankResult.setPay2bankOrder(pay2bankOrder);
            pay2bankResult.setPay2bankHead(response.getPay2bankHead());
            pay2bankResult.setResponseBody(response.getResponseBody());
            return pay2bankResult;
        } else {
            String rtnString = PKIUtil.byte2UTF8String(sealedData.getOriginalData());
            logger.info("解密后返回报文 = " + rtnString);
            return null;
        }
    }

    public static Pay2bankSearchResult unsealMsgPayQuery(String msg, String memberCode) throws Exception {
        Pay2bankSearchResponse response = XmlUtils.convertToJavaBean(msg, Pay2bankSearchResponse.class);
        SealedData sealedData = new SealedData();
        sealedData.setOriginalData(response.getSearchResponseBody().getSealDataType().getOriginalData() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getSearchResponseBody().getSealDataType().getOriginalData()));
        sealedData.setSignedData(response.getSearchResponseBody().getSealDataType().getSignedData() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getSearchResponseBody().getSealDataType().getSignedData()));
        sealedData.setEncryptedData(response.getSearchResponseBody().getSealDataType().getEncryptedData() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getSearchResponseBody().getSealDataType().getEncryptedData()));
        sealedData.setDigitalEnvelope(response.getSearchResponseBody().getSealDataType().getDigitalEnvelope() == null ? null : PKIUtil.utf8String2ByteWithBase64(response.getSearchResponseBody().getSealDataType().getDigitalEnvelope()));
        Mpf mpf = genMpf(memberCode);
        UnsealedData unsealedData = null;
        try {
            ICryptoService service = CryptoServiceFactory.createCryptoService();
            unsealedData = service.unseal(mpf, sealedData);
        } catch (CryptoException e) {
            System.out.println(e);
        }
        byte[] decryptedData = unsealedData.getDecryptedData();
        if (null != decryptedData) {
            String rtnString = PKIUtil.byte2UTF8String(decryptedData);
            logger.info("解密后返回报文 = " + rtnString);
            return XmlUtils.convertToJavaBean(rtnString, Pay2bankSearchResult.class);
        } else {
            String rtnString = PKIUtil.byte2UTF8String(sealedData.getOriginalData());
            logger.info("解密后返回报文 = " + rtnString);
            return null;
        }
    }

    public static Pay2bankNotify unsealOrderPayNotice(NotifyRequest request, String memberCode) {
        SealedData sealedData = new SealedData();
        sealedData.setOriginalData(request.getNotifyRequestBody().getSealDataType().getOriginalData() == null ? null : PKIUtil.utf8String2ByteWithBase64(request.getNotifyRequestBody().getSealDataType().getOriginalData()));
        sealedData.setSignedData(request.getNotifyRequestBody().getSealDataType().getSignedData() == null ? null : PKIUtil.utf8String2ByteWithBase64(request.getNotifyRequestBody().getSealDataType().getSignedData()));
        sealedData.setEncryptedData(request.getNotifyRequestBody().getSealDataType().getEncryptedData() == null ? null : PKIUtil.utf8String2ByteWithBase64(request.getNotifyRequestBody().getSealDataType().getEncryptedData()));
        sealedData.setDigitalEnvelope(request.getNotifyRequestBody().getSealDataType().getDigitalEnvelope() == null ? null : PKIUtil.utf8String2ByteWithBase64(request.getNotifyRequestBody().getSealDataType().getDigitalEnvelope()));
        Mpf mpf = genMpf(memberCode);
        UnsealedData unsealedData = null;
        try {
            ICryptoService service = CryptoServiceFactory.createCryptoService();
            unsealedData = service.unseal(mpf, sealedData);
        } catch (CryptoException e) {
            System.out.println(e);
        }
        byte[] decryptedData = unsealedData.getDecryptedData();
        if (null != decryptedData) {
            String rtnString = PKIUtil.byte2UTF8String(decryptedData);
            logger.info("解密后返回报文 = " + rtnString);
            return XmlUtils.convertToJavaBean(rtnString, Pay2bankNotify.class);
        } else {
            String rtnString = PKIUtil.byte2UTF8String(sealedData.getOriginalData());
            logger.info("解密后返回报文 = " + rtnString);
            return null;
        }
    }

    /**
     * 获取异步通知内容
     *
     * @param httpRequest
     * @return
     */
    public static String genNoticeRequestXml(HttpServletRequest httpRequest) {
        String line = null;
        ServletInputStream is = null;
        InputStreamReader isr = null;
        BufferedReader br = null;
        StringBuffer sb = new StringBuffer();
        try {
            is = httpRequest.getInputStream();
            isr = new InputStreamReader(is, "utf-8");
            br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            logger.error("genRequestXml exception", e);
        } finally {
            try {
                if (null != is) {
                    is.close();
                }
                if (null != isr) {
                    isr.close();
                }
                if (null != br) {
                    br.close();
                }
            } catch (Exception e) {
                logger.error("io close exception", e);
            }
        }
        return sb.toString();
    }

    public static Mpf genMpf(String memberCode) {
        Mpf mpf = new Mpf();
        mpf.setFeatureCode(fetureCode);
        mpf.setMemberCode(memberCode);
        return mpf;
    }
}
