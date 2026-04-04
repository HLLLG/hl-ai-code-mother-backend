package com.hl.hlaicodemother.bizmq;

/**
 * 截图异步任务使用的 RabbitMQ 名称，需与 {@link ScreenshotInitMain} 声明的交换机、队列、绑定一致。
 */
public final class ScreenshotMqConstants {

    public static final String EXCHANGE = "screenshot_exchange";
    public static final String QUEUE = "screenshot_queue";
    /** direct 交换机投递使用的 routing key（不是队列名） */
    public static final String ROUTING_KEY = "screenshot_routingKey";

    private ScreenshotMqConstants() {
    }
}
