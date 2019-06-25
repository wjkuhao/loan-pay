package com.mod.loan.itf.pay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mod.loan.common.message.OrderPayQueryMessage;
import com.mod.loan.model.OrderPay;
import com.mod.loan.service.KuaiqianService;
import com.mod.loan.service.OrderChangjiePayService;
import com.mod.loan.service.OrderPayService;
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

@Component
public class PayQueryConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PayQueryConsumer.class);

    @Autowired
    private OrderPayService orderPayService;
    @Autowired
    private KuaiqianService kuaiqianService;
    @Autowired
    OrderChangjiePayService orderChangjiePayService;

    @RabbitListener(queues = "queue_order_pay_query", containerFactory = "order_pay_query")
    @RabbitHandler
    public void order_pay_query(Message mess) {
        OrderPayQueryMessage payResultMessage = JSONObject.parseObject(mess.getBody(), OrderPayQueryMessage.class);
        OrderPay orderPay = orderPayService.selectByPrimaryKey(payResultMessage.getPayNo());
        // throw NullPointerException
        if (null == orderPay) {
            logger.error("放款记录不存在, message: {}", JSON.toJSONString(payResultMessage));
            return;
        }
        //
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
            case 4:
                orderPayService.yeePayQuery(payResultMessage);
                break;
            case 5:
                orderChangjiePayService.changjiePayQuery(payResultMessage);
                break;
            case 6:
                kuaiqianService.kuaiqianPayQuery(payResultMessage);
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
