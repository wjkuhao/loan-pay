package com.mod.loan.itf.pay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.mod.loan.common.message.OrderPayQueryMessage;
import com.mod.loan.model.OrderPay;
import com.mod.loan.service.OrderPayService;

@Component
public class PayQueryConsumer {

	private static final Logger logger = LoggerFactory.getLogger(PayQueryConsumer.class);

	@Autowired
	private OrderPayService orderPayService;

	@RabbitListener(queues = "queue_order_pay_query", containerFactory = "order_pay_query")
	@RabbitHandler
	public void order_pay_query(Message mess) {
		OrderPayQueryMessage payResultMessage = JSONObject.parseObject(mess.getBody(), OrderPayQueryMessage.class);
		OrderPay orderPay = orderPayService.selectByPrimaryKey(payResultMessage.getPayNo());
		switch (orderPay.getPayType()) {
		case 1:
			orderPayService.helibaoPayQuery(payResultMessage);
			break;
		case 2:
			orderPayService.fuyouPayQuery(payResultMessage);
			break;
		case 3:
			orderPayService.huijuPayQuery(payResultMessage);
			break;
		default:
			logger.error("放款查询消息payType异常,payResultMessage={}", payResultMessage);
			break;
		}
	}

	@Bean("order_pay_query")
	public SimpleRabbitListenerContainerFactory pointTaskContainerFactoryLoan(ConnectionFactory connectionFactory) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setPrefetchCount(1);
		factory.setConcurrentConsumers(5);
		return factory;
	}
}