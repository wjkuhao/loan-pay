package com.mod.loan.service.impl;

import com.mod.loan.model.request.TransCode4PayRequest;
import com.mod.loan.model.request.TransCode4QueryRequest;
import com.mod.loan.service.ChangjiePayService;
import com.mod.loan.util.changjie.BaseConstant;
import com.mod.loan.util.changjie.BaseParameter;
import com.mod.loan.util.changjie.ChanPayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

/**
 * @author NIELIN
 * @version $Id: ChangjiePayServiceImpl.java, v 0.1 2019/1/17 16:08 NIELIN Exp $
 */
@Service
public class ChangjiePayServiceImpl implements ChangjiePayService {
    private static Logger logger = LoggerFactory.getLogger(ChangjiePayServiceImpl.class);

    @Value("${changjie.url}")
    String changjieUrl;
    @Value("${changjie.Version}")
    String changjieVersion;
    @Value("${changjie.payOrRepayOrQuery}")
    String changjiePayOrRepayOrQuery;
    @Value("${changjie.transCode4Pay}")
    String changjieTransCode4Pay;
    @Value("${changjie.transCode4Query}")
    String changjieTransCode4Query;
    @Value("${changjie.payCallbackUrl}")
    String changjiePayCallbackUrl;

    @Override
    public String transCode4Pay(TransCode4PayRequest request) {
        if (null == request || StringUtils.isEmpty(request.getRequestSeriesNo()) || StringUtils.isEmpty(request.getName()) || StringUtils.isEmpty(request.getBankName())
                || StringUtils.isEmpty(request.getBankCardNo()) || null == request.getAmount() || request.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || StringUtils.isEmpty(request.getPartnerId()) || StringUtils.isEmpty(request.getPublicKey()) || StringUtils.isEmpty(request.getPrivateKey())) {
            logger.info("参数为空");
            return null;
        }
        //组装公共请求参数
        Map<String, String> origMap = BaseParameter.requestBaseParameter(changjiePayOrRepayOrQuery, changjieVersion, request.getPartnerId());
        //组装业务参数
        //交易码
        origMap.put("TransCode", changjieTransCode4Pay);
        //String trxId = StringUtil.getOrderNumber("p");
        //订单号
        origMap.put("OutTradeNo", request.getRequestSeriesNo());
        //业务类型(0=私人，1=公司)
        origMap.put("BusinessType", "0");
        //银行名称
        origMap.put("BankCommonName", request.getBankName());
        //银行卡号
        origMap.put("AcctNo", ChanPayUtil.encrypt(request.getBankCardNo(), request.getPublicKey(), BaseConstant.CHARSET));
        //姓名
        origMap.put("AcctName", ChanPayUtil.encrypt(request.getName(), request.getPublicKey(), BaseConstant.CHARSET));
        //账户类型(00=银行卡，01=存折，02=信用卡。不填默认为银行卡00。)
        origMap.put("AccountType", "00");
        //交易金额
        origMap.put("TransAmt", request.getAmount().setScale(2, BigDecimal.ROUND_HALF_UP).toString());
        //回调地址
        origMap.put("CorpPushUrl", changjiePayCallbackUrl);
        logger.info("#[准备调畅捷单笔代付放款的请求参数]-origMap={}", origMap);
        String result = ChanPayUtil.sendPost(origMap, BaseConstant.CHARSET, request.getPrivateKey(), changjieUrl);
        logger.info("#[准备调畅捷单笔代付放款的返回结果]-result={}", result);
        return result;
    }

    @Override
    public String transCode4Query(TransCode4QueryRequest request) {
        if (null == request || StringUtils.isEmpty(request.getRequestSeriesNo()) || StringUtils.isEmpty(request.getSeriesNo())
                || StringUtils.isEmpty(request.getPartnerId()) || StringUtils.isEmpty(request.getPrivateKey())) {
            logger.info("参数为空");
            return null;
        }
        //组装公共请求参数
        Map<String, String> origMap = BaseParameter.requestBaseParameter(changjiePayOrRepayOrQuery, changjieVersion, request.getPartnerId());
        //组装业务参数
        //交易码
        origMap.put("TransCode", changjieTransCode4Query);
        //String trxId = StringUtil.getOrderNumber("p");
        //订单号
        origMap.put("OutTradeNo", request.getRequestSeriesNo());
        //原业务请求订单号
        origMap.put("OriOutTradeNo", request.getSeriesNo());
        logger.info("#[准备调畅捷单笔交易查询的请求参数]-origMap={}", origMap);
        String result = ChanPayUtil.sendPost(origMap, BaseConstant.CHARSET, request.getPrivateKey(), changjieUrl);
        logger.info("#[准备调畅捷单笔交易查询的返回结果]-result={}", result);
        return result;
    }
}
