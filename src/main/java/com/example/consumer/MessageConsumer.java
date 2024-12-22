package com.example.consumer;

import com.example.config.RabbitMQConfig;
import com.example.util.IdempotencyUtil;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Component
public class MessageConsumer {

    @Resource
    private IdempotencyUtil idempotencyUtil;

    /**
     * 消费普通队列消息
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)
    public void handleMessage(Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        
        try {
            // 幂等性检查
            if (idempotencyUtil.isProcessed(messageId)) {
                log.info("消息已经处理过，messageId：{}", messageId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 处理消息
            String msg = new String(message.getBody());
            log.info("收到消息：{}", msg);
            
            // 模拟业务处理
            processMessage(msg);
            
            // 标记消息为已处理
            idempotencyUtil.markAsProcessed(messageId);
            
            // 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            
        } catch (Exception e) {
            log.error("处理消息失败，messageId：{}", messageId, e);
            // 消息处理失败，重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }

    /**
     * 消费死信队列消息
     */
    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void handleDeadLetterMessage(Message message, Channel channel) throws IOException {
        try {
            String msg = new String(message.getBody());
            log.error("收到死信队列消息：{}", msg);
            
            // 处理死信消息
            processDeadLetterMessage(msg);
            
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.error("处理死信消息失败", e);
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
        }
    }

    /**
     * 消费死信队列消息（延迟消息）
     */
    @RabbitListener(queues = RabbitMQConfig.DEAD_LETTER_QUEUE)
    public void handleDeadLetterMessageDelayed(Message message, Channel channel) throws IOException {
        String messageId = message.getMessageProperties().getMessageId();
        
        try {
            // 幂等性检查
            if (idempotencyUtil.isProcessed(messageId)) {
                log.info("延迟消息已经处理过，messageId：{}", messageId);
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
                return;
            }

            // 处理消息
            String msg = new String(message.getBody());
            log.info("收到延迟消息：{}", msg);
            
            // 模拟业务处理
            processMessage(msg);
            
            // 标记消息为已处理
            idempotencyUtil.markAsProcessed(messageId);
            
            // 手动确认消息
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            
        } catch (Exception e) {
            log.error("处理延迟消息失败，messageId：{}", messageId, e);
            // 消息处理失败，直接丢弃（因为是死信队列，不需要再次重试）
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        }
    }

    private void processMessage(String message) {
        // 模拟消息处理
        try {
            Thread.sleep(1000);
            log.info("消息处理完成：{}", message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("消息处理被中断", e);
        }
    }

    private void processDeadLetterMessage(String message) {
        // 死信消息处理逻辑
        log.info("处理死信消息：{}", message);
    }
}
