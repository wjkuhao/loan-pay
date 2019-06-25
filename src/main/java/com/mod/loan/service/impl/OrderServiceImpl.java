package com.mod.loan.service.impl;

import com.mod.loan.common.mapper.BaseServiceImpl;
import com.mod.loan.mapper.OrderMapper;
import com.mod.loan.mapper.OrderPayMapper;
import com.mod.loan.model.Order;
import com.mod.loan.model.OrderPay;
import com.mod.loan.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderServiceImpl extends BaseServiceImpl<Order, Long> implements OrderService {
	
	@Autowired
	private OrderMapper orderMapper;
	@Autowired
	private OrderPayMapper orderPayMapper;

	@Override
	public void updatePayInfo(Order order, OrderPay orderPay) {
		if (order != null) {
			orderMapper.updateByPrimaryKeySelective(order);
		}
		orderPayMapper.insertSelective(orderPay);
	}

	@Override
	public void updatePayCallbackInfo(Order order, OrderPay orderPay) {
		orderMapper.updateByPrimaryKeySelective(order);
		orderPayMapper.updateByPrimaryKeySelective(orderPay);
	}

	@Override
	public int countOrderPaySuccessRecord(Long orderId) {
		return orderPayMapper.countOrderPaySuccessRecord(orderId);
	}

	@Override
	public int countOrderPaySuccessOneDay(Long uid) {
		return orderMapper.countOrderPaySuccessOneDay(uid);
	}

    @Override
    public Map selectOrderByPayNoAndAlias(String payNo, String alias) {
        return orderMapper.selectOrderByPayNoAndAlias(payNo, alias);
    }

}
