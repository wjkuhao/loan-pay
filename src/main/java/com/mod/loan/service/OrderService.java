package com.mod.loan.service;

import com.mod.loan.common.mapper.BaseService;
import com.mod.loan.model.Order;
import com.mod.loan.model.OrderPay;

import java.util.Map;

public interface OrderService extends BaseService<Order, Long> {

	void updatePayInfo(Order order, OrderPay orderPay);

	void updatePayCallbackInfo(Order order, OrderPay orderPay);

	/**
	 * 统计订单放款成功记录
	 * 
	 * @param orderId
	 * @return
	 */
	int countOrderPaySuccessRecord(Long orderId);

	int countOrderPaySuccessOneDay(Long uid);

    /**
     * 根据放款流水号和商户别名查看订单
     *
     * @param payNo
     * @param alias
     * @return
     */
    Map selectOrderByPayNoAndAlias(String payNo, String alias);

}
