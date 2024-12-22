package com.example.controller;

import com.example.producer.MessageProducer;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/message")
public class MessageController {

    @Resource
    private MessageProducer messageProducer;

    @PostMapping("/send")
    public String sendMessage(@RequestBody String message) {
        messageProducer.sendMessage(message);
        return "消息发送成功";
    }

    @PostMapping("/send/delayed")
    public String sendDelayedMessage(@RequestBody String message, @RequestParam int delayTime) {
        messageProducer.sendDelayedMessage(message, delayTime);
        return "延迟消息发送成功";
    }
}
