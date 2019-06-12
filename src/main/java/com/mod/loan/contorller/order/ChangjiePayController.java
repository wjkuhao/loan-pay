package com.mod.loan.contorller.order;

import com.mod.loan.common.enums.ResponseEnum;
import com.mod.loan.common.model.ResultMessage;
import com.mod.loan.model.request.TransCode4PayRequest;
import com.mod.loan.model.request.TransCode4QueryRequest;
import com.mod.loan.service.ChangjiePayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 畅捷支付
 *
 * @author NIELIN
 * @version $Id: ChangjiePayController.java, v 0.1 2019/1/17 16:08 NIELIN Exp $
 */
@RestController
@RequestMapping("changjiePay")
public class ChangjiePayController {
    private static Logger logger = LoggerFactory.getLogger(ChangjiePayController.class);

    @Autowired
    ChangjiePayService changjiePayService;

    /**
     * 单笔代付放款
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "transCode4Pay")
    public ResultMessage transCode4Pay(@RequestBody TransCode4PayRequest request) {
        logger.info("#[单笔代付放款]-[开始]-request={}", request);
        if (null == request || StringUtils.isEmpty(request.getRequestSeriesNo()) || StringUtils.isEmpty(request.getBankCardNo()) || StringUtils.isEmpty(request.getName())
                || StringUtils.isEmpty(request.getBankName()) || null == request.getAmount() || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return new ResultMessage(ResponseEnum.M5000);
        }
        try {
            String sd = changjiePayService.transCode4Pay(request);
            logger.info("#[单笔代付放款]-[结束]-sd={}", sd);
            return new ResultMessage(ResponseEnum.M2000, sd);
        } catch (Exception e) {
            logger.error("#[单笔代付放款]-[异常]-e={}", e);
            return new ResultMessage(ResponseEnum.M4000);
        }
    }

    /**
     * 单笔交易查询
     *
     * @param request
     * @return
     */
    @RequestMapping(value = "transCode4Query")
    public ResultMessage transCode4Query(@RequestBody TransCode4QueryRequest request) {
        logger.info("#[单笔交易查询]-[开始]-request={}", request);
        if (null == request || StringUtils.isEmpty(request.getRequestSeriesNo()) || StringUtils.isEmpty(request.getSeriesNo())) {
            return new ResultMessage(ResponseEnum.M5000);
        }
        try {
            String sd = changjiePayService.transCode4Query(request);
            logger.info("#[单笔交易查询]-[结束]-sd={}", sd);
            return new ResultMessage(ResponseEnum.M2000, sd);
        } catch (Exception e) {
            logger.error("#[单笔交易查询]-[异常]-e={}", e);
            return new ResultMessage(ResponseEnum.M4000);
        }
    }

}
