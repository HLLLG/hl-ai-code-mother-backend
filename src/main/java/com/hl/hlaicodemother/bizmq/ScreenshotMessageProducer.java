package com.hl.hlaicodemother.bizmq;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 消息生产者
 */
@Slf4j
@Component
public class ScreenshotMessageProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void sendMessage(String exchange, String routingKey, String message) {
        log.info("send message: {}", message);
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }

    /**
     * 发送部署后截图任务，routing key 与 {@link ScreenshotMqConstants#ROUTING_KEY}、{@link ScreenshotInitMain} 绑定一致。
     */
    public void sendScreenshotTask(ScreenshotTaskMessage task) {
        log.info("send screenshot task, appId={}", task.getAppId());
        rabbitTemplate.convertAndSend(
                ScreenshotMqConstants.EXCHANGE,
                ScreenshotMqConstants.ROUTING_KEY,
                JSONUtil.toJsonStr(task));
    }
}