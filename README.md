# RabbitMQ 延迟消息示例项目

这是一个基于Spring Boot和RabbitMQ的消息队列示例项目，演示了如何使用TTL（Time-To-Live）和死信队列来实现延迟消息功能。

## 功能特性

- 支持发送普通消息
- 支持发送延迟消息（基于TTL和死信队列实现）
- 实现了消息确认机制
- 支持消息幂等性处理
- 完善的异常处理和日志记录

## 技术栈

- Spring Boot 2.x
- RabbitMQ 3.x
- Java 8+
- Maven

## 项目结构

```
src/main/java/com/example/
├── config/
│   └── RabbitMQConfig.java          # RabbitMQ配置类
├── controller/
│   └── MessageController.java       # 消息发送接口
├── producer/
│   └── MessageProducer.java         # 消息生产者
├── consumer/
│   └── MessageConsumer.java         # 消息消费者
├── util/
│   └── IdempotencyUtil.java         # 幂等性工具类
└── RabbitMQDemoApplication.java     # 应用启动类
```

## 消息队列设计

### 队列结构
1. 普通队列（normal_queue）：用于处理普通消息
2. TTL队列（ttl_queue）：设置了消息过期时间的队列，用于实现延迟消息
3. 死信队列（dead_letter_queue）：接收过期的消息，实现延迟处理

### 工作流程
1. 普通消息：直接发送到普通队列，立即被消费
2. 延迟消息：
   - 消息首先发送到TTL队列
   - 消息在TTL队列中等待30秒后过期
   - 过期的消息自动转发到死信队列
   - 死信队列的消费者处理这些延迟消息

## API接口

### 发送普通消息
```http
POST /api/message/send
Content-Type: application/json

"这是一条普通消息"
```

### 发送延迟消息
```http
POST /api/message/send/delayed
Content-Type: application/json

"这是一条延迟消息"
```

## 配置说明

主要配置在`application.yml`文件中：

```yaml
spring:
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: guest
    password: guest
    connection-timeout: 10000
    publisher-confirm-type: correlated
    publisher-returns: true
    template:
      mandatory: true
      retry:
        enabled: true
        initial-interval: 1000
        max-attempts: 3
        max-interval: 10000
```

## 特性说明

### 消息确认机制
- 生产者确认：通过`publisher-confirm-type`和`publisher-returns`实现
- 消费者确认：使用手动ACK模式，确保消息处理的可靠性

### 幂等性处理
- 使用消息ID进行幂等性检查
- 防止重复消息的处理

### 异常处理
- 生产者：消息发送失败时进行日志记录
- 消费者：消息处理失败时可以选择重试或直接丢弃

## 运行说明

1. 确保已安装并启动RabbitMQ服务
2. 配置application.yml中的RabbitMQ连接信息
3. 运行RabbitMQDemoApplication启动应用
4. 使用API接口发送测试消息

## 注意事项

1. TTL队列的延迟时间固定为30秒，可以通过修改配置调整
2. 确保RabbitMQ服务正常运行且可访问
3. 建议在开发环境中启用RabbitMQ管理界面（默认端口15672）进行监控

## 扩展建议

1. 可以创建多个不同TTL时间的队列，支持不同的延迟需求
2. 可以添加消息持久化机制
3. 可以实现更复杂的重试策略
4. 可以添加消息监控和告警机制
