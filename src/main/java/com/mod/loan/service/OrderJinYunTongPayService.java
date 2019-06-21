package com.mod.loan.service;

import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.common.message.OrderPayQueryMessage;

public interface OrderJinYunTongPayService {
    /**
    * 金运通打款
    * @Author actor
    * @Date 2019/6/20 16:39
    */
    void pay(OrderPayMessage orderPayMessage);
    /**
    * 金运通打款查询
    * @Author actor
    * @Date 2019/6/20 16:39
    */
    void payQuery(OrderPayQueryMessage orderPayMessage);
}
