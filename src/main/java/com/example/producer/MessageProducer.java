package com.example.producer;

import com.example.config.RabbitMQConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@Component
public class MessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送普通消息
     */
    public void sendMessage(Object message) {
        String messageId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(messageId);
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.ROUTING_KEY,
            message,
            correlationData
        );
        
        log.info("发送消息到普通队列，消息ID：{}，内容：{}", messageId, message);
    }

    /**
     * 发送延迟消息
     */
    public void sendDelayedMessage(Object message, int delayTime) {
        String messageId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(messageId);
        
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE_NAME,
            RabbitMQConfig.TTL_ROUTING_KEY,
            message,
            correlationData
        );
        
        log.info("发送消息到TTL队列，消息ID：{}，内容：{}, 延迟时间：{}ms", messageId, message, delayTime);
    }
}
