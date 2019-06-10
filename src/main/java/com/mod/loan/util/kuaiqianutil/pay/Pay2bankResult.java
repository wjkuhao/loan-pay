package com.mod.loan.util.kuaiqianutil.pay;

/**
 * 报文实体
 * @author zhiwei.ma
 *
 */
public class Pay2bankResult {

	private Pay2bankHead pay2bankHead;
	
	private ResponseBody responseBody;

	private Pay2bankOrder pay2bankOrder;

	public Pay2bankHead getPay2bankHead() {
		return pay2bankHead;
	}

	public void setPay2bankHead(Pay2bankHead pay2bankHead) {
		this.pay2bankHead = pay2bankHead;
	}

	public ResponseBody getResponseBody() {
		return responseBody;
	}

	public void setResponseBody(ResponseBody responseBody) {
		this.responseBody = responseBody;
	}

	public Pay2bankOrder getPay2bankOrder() {
		return pay2bankOrder;
	}

	public void setPay2bankOrder(Pay2bankOrder pay2bankOrder) {
		this.pay2bankOrder = pay2bankOrder;
	}
}
