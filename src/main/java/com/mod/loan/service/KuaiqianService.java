package com.mod.loan.service;

import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.common.message.OrderPayQueryMessage;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface KuaiqianService {

    void kuaiqianPay(OrderPayMessage payMessage);

    void kuaiqianPayQuery(OrderPayQueryMessage payResultMessage);
}
