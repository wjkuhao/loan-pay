package com.mod.loan.service;

import com.mod.loan.common.mapper.BaseService;
import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.common.message.OrderPayQueryMessage;
import com.mod.loan.model.OrderPay;

public interface OrderPayService extends BaseService<OrderPay, String> {

	/**
	 * 合利宝放款方法
	 * 
	 * @param payMessage
	 */
	void helibaoPay(OrderPayMessage payMessage);

	/**
	 * 富友放款方法
	 * 
	 * @param payMessage
	 */
	void fuyouPay(OrderPayMessage payMessage);

	/**
	 * 汇聚放款方法
	 * 
	 * @param payMessage
	 */
	void huijuPay(OrderPayMessage payMessage);

	/**
	 * 合利宝放款查询方法
	 * 
	 * @param payResultMessage
	 */
	void helibaoPayQuery(OrderPayQueryMessage payResultMessage);

	/**
	 * 富友放款查询方法
	 * 
	 * @param payResultMessage
	 */
	void fuyouPayQuery(OrderPayQueryMessage payResultMessage);

	/**
	 * 汇聚放款查询方法
	 * 
	 * @param payResultMessage
	 */
	void huijuPayQuery(OrderPayQueryMessage payResultMessage);
}
