package com.hl.hlaicodemother.bizmq;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ScreenshotMessageProducerTest {

    @Resource
    private ScreenshotMessageProducer screenshotMessageProducer;

    @Test
    void sendMessage() {
        screenshotMessageProducer.sendMessage("code_exchange", "my_routingKey", "hello world");
    }


}