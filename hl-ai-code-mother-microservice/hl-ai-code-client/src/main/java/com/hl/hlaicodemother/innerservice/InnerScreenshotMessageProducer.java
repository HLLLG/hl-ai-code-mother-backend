package com.hl.hlaicodemother.innerservice;

public interface InnerScreenshotMessageProducer {

    void sendMessage(String exchange, String routingKey, String message);

    void sendScreenshotTask(com.hl.hlaicodemother.model.ScreenshotTaskMessage task);
}
