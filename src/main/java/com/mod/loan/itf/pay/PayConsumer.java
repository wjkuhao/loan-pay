package com.mod.loan.itf.pay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mod.loan.common.message.OrderPayMessage;
import com.mod.loan.config.redis.RedisConst;
import com.mod.loan.config.redis.RedisMapper;
import com.mod.loan.service.*;
import org.apache.commons.lang.StringUtils;
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
public class PayConsumer {

    private static final Logger logger = LoggerFactory.getLogger(PayConsumer.class);

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderPayService orderPayService;
    @Autowired
    private KuaiqianService kuaiqianService;
    @Autowired
    private RedisMapper redisMapper;
    @Autowired
    private OrderChangjiePayService orderChangjiePayService;
    @Autowired
    private OrderJinYunTongPayService orderJinYunTongPayService;

    @RabbitListener(queues = "queue_order_pay", containerFactory = "order_pay")
    @RabbitHandler
    public void order_pay(Message mess) {
        OrderPayMessage payMessage = JSONObject.parseObject(mess.getBody(), OrderPayMessage.class);
        if (!redisMapper.lock(RedisConst.ORDER_LOCK + payMessage.getOrderId(), 3600)) {
            logger.error("放款消息重复,message={}", JSON.toJSONString(payMessage));
            return;
        }
        if (orderService.countOrderPaySuccessRecord(payMessage.getOrderId()) > 0) {
            logger.error("orderId={}已存在放款成功记录,本次放款取消", payMessage.getOrderId());
            return;
        }
        String payType = StringUtils.isNotBlank(payMessage.getPayType()) ? payMessage.getPayType() : "";
        switch (payType) {
            case "helibao":
                orderPayService.helibaoPay(payMessage);
                break;
            case "fuyou":
                orderPayService.fuyouPay(payMessage);
                break;
            case "huiju":
                orderPayService.huijuPay(payMessage);
                break;
            case "yeepay":
                orderPayService.yeePay(payMessage);
                break;
            case "kuaiqian":
                kuaiqianService.kuaiqianPay(payMessage);
                break;
            case "changjie":
                orderChangjiePayService.changjiePay(payMessage);
                break;
            case "jinyuntong":
                orderJinYunTongPayService.pay(payMessage);
                break;
            default:
                logger.error("放款消息payType异常,payMessage={}", payMessage);
                break;
        }
    }

    @Bean("order_pay")
    public SimpleRabbitListenerContainerFactory pointTaskContainerFactoryLoan(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setPrefetchCount(1);
        factory.setConcurrentConsumers(1);
        return factory;
    }
}
