package com.example.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitMQConfig {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    // 普通队列名称
    public static final String QUEUE_NAME = "normal_queue";
    // 死信队列名称
    public static final String DEAD_LETTER_QUEUE = "dead_letter_queue";
    // 延迟队列名称（TTL队列）
    public static final String TTL_QUEUE_NAME = "ttl_queue";
    // 交换机名称
    public static final String EXCHANGE_NAME = "normal_exchange";
    // 死信交换机名称
    public static final String DEAD_LETTER_EXCHANGE = "dead_letter_exchange";
    // 路由键
    public static final String ROUTING_KEY = "normal_routing_key";
    // 死信路由键
    public static final String DEAD_LETTER_ROUTING_KEY = "dead_letter_routing_key";
    // TTL路由键
    public static final String TTL_ROUTING_KEY = "ttl_routing_key";

    @Bean
    public ConnectionFactory connectionFactory() {
        return new CachingConnectionFactory();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(true);
        return admin;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
        rabbitTemplate.setMandatory(true);
        
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.info("消息发送成功: correlationData={}", correlationData);
            } else {
                log.error("消息发送失败: correlationData={}, cause={}", correlationData, cause);
            }
        });
        
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息被退回: message={}, replyCode={}, replyText={}, exchange={}, routingKey={}",
                    returned.getMessage(), returned.getReplyCode(), returned.getReplyText(),
                    returned.getExchange(), returned.getRoutingKey());
        });
        
        return rabbitTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            // 声明交换机
            rabbitAdmin.declareExchange(normalExchange());
            rabbitAdmin.declareExchange(deadLetterExchange());
            
            // 声明队列
            rabbitAdmin.declareQueue(normalQueue());
            rabbitAdmin.declareQueue(deadLetterQueue());
            rabbitAdmin.declareQueue(ttlQueue());
            
            // 声明绑定关系
            rabbitAdmin.declareBinding(normalBinding());
            rabbitAdmin.declareBinding(deadLetterBinding());
            rabbitAdmin.declareBinding(ttlBinding());
            
            log.info("RabbitMQ 交换机和队列初始化完成");
        } catch (Exception e) {
            log.error("RabbitMQ 初始化失败", e);
            throw e;
        }
    }

    @Bean
    public DirectExchange normalExchange() {
        return ExchangeBuilder.directExchange(EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue normalQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        return QueueBuilder.durable(QUEUE_NAME)
                .withArguments(args)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue ttlQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", DEAD_LETTER_ROUTING_KEY);
        // 设置队列中消息的过期时间为30秒
        args.put("x-message-ttl", 30000);
        return QueueBuilder.durable(TTL_QUEUE_NAME)
                .withArguments(args)
                .build();
    }

    @Bean
    public Binding normalBinding() {
        return BindingBuilder.bind(normalQueue())
                .to(normalExchange())
                .with(ROUTING_KEY);
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(DEAD_LETTER_ROUTING_KEY);
    }

    @Bean
    public Binding ttlBinding() {
        return BindingBuilder.bind(ttlQueue())
                .to(normalExchange())
                .with(TTL_ROUTING_KEY);
    }
}
