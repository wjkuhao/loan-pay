package com.mod.loan.common.message;

/**
 * 放款队列的DTO
 */
public class OrderPayMessage {

	private Long orderId; // 订单Id
	private String payType;// 放款渠道

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public String getPayType() {
		return payType;
	}

	public void setPayType(String payType) {
		this.payType = payType;
	}

	@Override
	public String toString() {
		return "OrderPayMessage [orderId=" + orderId + ", payType=" + payType + "]";
	}

}
