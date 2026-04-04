package com.hl.hlaicodemother.bizmq;

import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class ScreenshotInitMain {

    public static void main(String[] args) {
        try {
            // 创建连接工厂
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // 声明交换机、队列、绑定（名称与 ScreenshotMqConstants 一致）
            channel.exchangeDeclare(ScreenshotMqConstants.EXCHANGE, "direct");
            channel.queueDeclare(ScreenshotMqConstants.QUEUE, true, false, false, null);
            channel.queueBind(ScreenshotMqConstants.QUEUE, ScreenshotMqConstants.EXCHANGE, ScreenshotMqConstants.ROUTING_KEY);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化MQ异常");
        }
    }
}
