package com.example.util;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class IdempotencyUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String IDEMPOTENCY_KEY_PREFIX = "mq:idempotency:";
    private static final long EXPIRATION_TIME = 24; // 过期时间（小时）

    /**
     * 检查消息是否已经处理过
     * @param messageId 消息ID
     * @return true if message has been processed before
     */
    /**
     * 检查消息是否已经处理过
     * 通过Redis的setIfAbsent命令来实现幂等性检查
     * 如果key不存在（第一次处理），则设置key的值为1，且设置过期时间为24小时
     * 如果key存在（已经处理过），则setIfAbsent命令将返回false
     * @param messageId 消息ID
     * @return true if message has been processed before
     */
    public boolean isProcessed(String messageId) {
        String key = IDEMPOTENCY_KEY_PREFIX + messageId;
        Boolean result = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", EXPIRATION_TIME, TimeUnit.HOURS);
        return result != null && !result;
    }

    /**
     * 标记消息为已处理
     * @param messageId 消息ID
     */
    public void markAsProcessed(String messageId) {
        String key = IDEMPOTENCY_KEY_PREFIX + messageId;
        stringRedisTemplate.opsForValue().set(key, "1", EXPIRATION_TIME, TimeUnit.HOURS);
    }
}
