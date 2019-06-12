package com.mod.loan.service;

import com.mod.loan.common.mapper.BaseService;
import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.common.message.OrderPayQueryMessage;
import com.mod.loan.model.OrderPay;

public interface OrderChangjiePayService extends BaseService<OrderPay, String> {

    /**
     * 畅捷代付放款
     *
     * @param payMessage
     */
    void changjiePay(OrderPayMessage payMessage);

    /**
     * 畅捷代付放款查询
     *
     * @param payResultMessage
     */
    void changjiePayQuery(OrderPayQueryMessage payResultMessage);

}
