package com.mod.loan.util.jinyuntong;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * TC2002接口请求bean
 */
public class TC2002_ReqBodyBean {

	@NotNull(message="原交易流水号不能为空")
	@Size(min = 1, max = 32,message="原交易流水号不在1-32个字符范围内")
	private String oriTranFlowid;

	public String getOriTranFlowid() {
		return oriTranFlowid;
	}

	public void setOriTranFlowid(String oriTranFlowid) {
		this.oriTranFlowid = oriTranFlowid;
	}
}
