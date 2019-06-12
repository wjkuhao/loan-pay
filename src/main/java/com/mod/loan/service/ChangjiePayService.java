package com.mod.loan.service;

import com.mod.loan.model.request.TransCode4PayRequest;
import com.mod.loan.model.request.TransCode4QueryRequest;

/**
 * @author NIELIN
 * @version $Id: ChangjiePayService.java, v 0.1 2019/1/17 16:08 NIELIN Exp $
 */
public interface ChangjiePayService {

    /**
     * 单笔代付放款
     *
     * @param request
     * @return
     */
    String transCode4Pay(TransCode4PayRequest request) throws Exception;

    /**
     * 单笔交易查询
     *
     * @param request
     * @return
     */
    String transCode4Query(TransCode4QueryRequest request) throws Exception;


}
